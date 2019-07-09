/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class PropertiesLowering(
    private val context: BackendContext,
    private val originOfSyntheticMethodForAnnotations: IrDeclarationOrigin? = null,
    private val skipExternalProperties: Boolean = false,
    private val generateAnnotationFields: Boolean = false,
    private val computeSyntheticMethodName: ((Name) -> String)? = null
) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitFile(declaration: IrFile): IrFile {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { lowerProperty(it, ClassKind.CLASS) }
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { lowerProperty(it, declaration.kind) }
        return declaration
    }

    private fun lowerProperty(declaration: IrDeclaration, kind: ClassKind): List<IrDeclaration>? =
        if (declaration is IrProperty)
            if (skipExternalProperties && declaration.isEffectivelyExternal()) listOf(declaration) else {
                ArrayList<IrDeclaration>(4).apply {
                    // JvmFields in a companion object refer to companion's owners and should not be generated within companion.
                    if (generateAnnotationFields || (kind != ClassKind.ANNOTATION_CLASS && declaration.backingField?.parent == declaration.parent)) {
                        addIfNotNull(declaration.backingField)
                    }
                    addIfNotNull(declaration.getter)
                    addIfNotNull(declaration.setter)

                    if (declaration.annotations.isNotEmpty() && originOfSyntheticMethodForAnnotations != null
                        && computeSyntheticMethodName != null
                    ) {
                        val methodName = computeSyntheticMethodName.invoke(declaration.name) // Workaround KT-4113
                        add(createSyntheticMethodForAnnotations(declaration, originOfSyntheticMethodForAnnotations, methodName))
                    }
                }
            }
        else
            null

    private fun createSyntheticMethodForAnnotations(declaration: IrProperty, origin: IrDeclarationOrigin, name: String): IrFunctionImpl {
        val descriptor = WrappedSimpleFunctionDescriptor(declaration.descriptor.annotations)
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)
        // TODO: ACC_DEPRECATED
        return IrFunctionImpl(
            -1, -1, origin, symbol, Name.identifier(name),
            Visibilities.PUBLIC, Modality.OPEN, context.irBuiltIns.unitType,
            isInline = false, isExternal = false, isTailrec = false, isSuspend = false
        ).apply {
            descriptor.bind(this)

            extensionReceiverParameter = declaration.getter?.extensionReceiverParameter?.copyTo(this)

            body = IrBlockBodyImpl(-1, -1)

            annotations.addAll(declaration.annotations)
            metadata = declaration.metadata
        }
    }

    companion object {
        fun checkNoProperties(irFile: IrFile) {
            irFile.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitProperty(declaration: IrProperty) {
                    error("No properties should remain at this stage")
                }
            })
        }
    }
}

class LocalDelegatedPropertiesLowering : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        declaration.transformChildrenVoid(this)

        val initializer = declaration.delegate.initializer!!
        declaration.delegate.initializer = IrBlockImpl(
            initializer.startOffset, initializer.endOffset, initializer.type, null,
            listOfNotNull(
                declaration.getter,
                declaration.setter,
                initializer
            )
        )

        return declaration.delegate
    }
}
