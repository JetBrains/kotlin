/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.inline.SyntheticAccessorGenerator
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.concurrent.ConcurrentHashMap

class JvmSyntheticAccessorGenerator(context: JvmBackendContext) : SyntheticAccessorGenerator<JvmBackendContext>(context) {

    override fun accessorModality(parent: IrDeclarationParent): Modality =
        if (parent is IrClass && parent.isJvmInterface) Modality.OPEN else Modality.FINAL

    override fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopes: List<ScopeWithIr>): IrDeclarationParent =
        if (visibility == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY) {
            val classes = scopes.map { it.irElement }.filterIsInstance<IrClass>()
            val companions = classes.mapNotNull(IrClass::companionObject)
            val objectsInScope =
                classes.flatMap { it.declarations.filter(IrDeclaration::isAnonymousObject).filterIsInstance<IrClass>() }
            val candidates = objectsInScope + companions + classes
            candidates.lastOrNull { parent is IrClass && it.isSubclassOf(parent) } ?: classes.last()
        } else {
            parent
        }

    override fun mapFunctionName(function: IrSimpleFunction): String = context.defaultMethodSignatureMapper.mapFunctionName(function)

    override fun functionAccessorSuffix(function: IrSimpleFunction, superQualifier: IrClassSymbol?, scopes: List<ScopeWithIr>): String {
        val currentClass = scopes.lastOrNull { it.scope.scopeOwnerSymbol is IrClassSymbol }?.irElement as? IrClass
        if (currentClass != null &&
            currentClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS &&
            currentClass.parentAsClass == function.parentAsClass
        ) {
            // The only function accessors placed on interfaces are for private functions and JvmDefault implementations.
            // The two cannot clash.
            return if (DescriptorVisibilities.isPrivate(function.visibility)) "" else "\$jd"
        }
        return super.functionAccessorSuffix(function, superQualifier, scopes)
    }

    override fun fieldGetterName(field: IrField): String = JvmAbi.getterName(field.name.asString())

    override fun fieldSetterName(field: IrField): String = JvmAbi.setterName(field.name.asString())

    override fun fieldAccessorSuffix(field: IrField, superQualifierSymbol: IrClassSymbol?): String {
        // Special _c_ompanion _p_roperty suffix for accessing companion backing field moved to outer
        if (field.origin == JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD && !field.parentAsClass.isCompanion) {
            return "cp"
        }
        return super.fieldAccessorSuffix(field, superQualifierSymbol)
    }

    override val DescriptorVisibility.isProtected: Boolean
        get() = AsmUtil.getVisibilityAccessFlag(delegate) == Opcodes.ACC_PROTECTED

    private fun getSyntheticConstructorAccessor(
        declaration: IrConstructor,
        constructorToAccessorMap: MutableMap<IrConstructor, IrConstructor>
    ): IrConstructor {
        return constructorToAccessorMap.getOrPut(declaration) {
            declaration.makeConstructorAccessor(JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR).also { accessor ->
                if (declaration.constructedClass.modality != Modality.SEALED) {
                    // There's a special case in the JVM backend for serializing the metadata of hidden
                    // constructors - we serialize the descriptor of the original constructor, but the
                    // signature of the accessor. We implement this special case in the JVM IR backend by
                    // attaching the metadata directly to the accessor. We also have to move all annotations
                    // to the accessor. Parameter annotations are already moved by the copyTo method.
                    if (declaration.metadata != null) {
                        accessor.metadata = declaration.metadata
                        declaration.metadata = null
                    }
                    accessor.annotations += declaration.annotations
                    declaration.annotations = emptyList()
                    declaration.valueParameters.forEach { it.annotations = emptyList() }
                }
            }
        }
    }

    fun isOrShouldBeHiddenSinceHasMangledParams(constructor: IrConstructor): Boolean {
        if (constructor in context.hiddenConstructorsWithMangledParams.keys) return true
        return constructor.isOrShouldBeHiddenDueToOrigin &&
                !DescriptorVisibilities.isPrivate(constructor.visibility) &&
                !constructor.constructedClass.isValue &&
                (context.multiFieldValueClassReplacements.originalConstructorForConstructorReplacement[constructor]
                    ?: constructor).hasMangledParameters() &&
                !constructor.constructedClass.isAnonymousObject
    }

    fun isOrShouldBeHiddenAsSealedClassConstructor(constructor: IrConstructor): Boolean {
        if (constructor in context.hiddenConstructorsOfSealedClasses.keys) return true
        return constructor.isOrShouldBeHiddenDueToOrigin &&
                constructor.visibility != DescriptorVisibilities.PUBLIC &&
                constructor.constructedClass.modality == Modality.SEALED
    }

    private val IrConstructor.isOrShouldBeHiddenDueToOrigin: Boolean
        get() = !(origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                origin == IrDeclarationOrigin.SYNTHETIC_ACCESSOR ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR ||
                origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB)

    fun getSyntheticConstructorWithMangledParams(declaration: IrConstructor) =
        getSyntheticConstructorAccessor(declaration, context.hiddenConstructorsWithMangledParams)

    fun getSyntheticConstructorOfSealedClass(declaration: IrConstructor) =
        getSyntheticConstructorAccessor(declaration, context.hiddenConstructorsOfSealedClasses)
}
