/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class JvmPropertiesLowering(private val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { lowerProperty(it, declaration.kind) }
        return declaration
    }

    private fun lowerProperty(declaration: IrDeclaration, kind: ClassKind): List<IrDeclaration>? =
        if (declaration is IrProperty)
            ArrayList<IrDeclaration>(4).apply {
                val field = declaration.backingField

                // JvmFields in a companion object refer to companion's owners and should not be generated within companion.
                if ((kind != ClassKind.ANNOTATION_CLASS || field?.isStatic == true) && field?.parent == declaration.parent) {
                    addIfNotNull(field)
                }
                addIfNotNull(declaration.getter)
                addIfNotNull(declaration.setter)

                if (!declaration.isFakeOverride && declaration.annotations.isNotEmpty()) {
                    add(createSyntheticMethodForAnnotations(declaration))
                }
            }
        else
            null

    private fun createSyntheticMethodForAnnotations(declaration: IrProperty): IrFunctionImpl {
        val descriptor = WrappedSimpleFunctionDescriptor(declaration.descriptor.annotations)
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)
        val name = computeSyntheticMethodName(declaration)
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS, symbol,
            Name.identifier(name), declaration.visibility, Modality.OPEN, context.irBuiltIns.unitType,
            isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isExpect = false, isFakeOverride = false,
            isOperator = false
        ).apply {
            descriptor.bind(this)

            val extensionReceiver = declaration.getter?.extensionReceiverParameter
            if (extensionReceiver != null) {
                // Use raw type of extension receiver to avoid generic signature, which would be useless for this method.
                extensionReceiverParameter = extensionReceiver.copyTo(this, type = extensionReceiver.type.classifierOrFail.typeWith())
            }

            body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            parent = declaration.parent

            annotations = declaration.annotations
            metadata = declaration.metadata
        }
    }

    private fun computeSyntheticMethodName(property: IrProperty): String {
        val baseName =
            if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm)) {
                property.getter?.let { getter ->
                    context.methodSignatureMapper.mapFunctionName(getter)
                } ?: JvmAbi.getterName(property.name.asString())
            } else {
                property.name.asString()
            }
        return JvmAbi.getSyntheticMethodNameForAnnotatedProperty(baseName)
    }
}
