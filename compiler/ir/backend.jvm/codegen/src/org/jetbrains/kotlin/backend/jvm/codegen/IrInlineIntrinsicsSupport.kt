/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.intrinsics.SignatureString
import org.jetbrains.kotlin.backend.jvm.ir.getCallableReferenceOwnerKClassType
import org.jetbrains.kotlin.backend.jvm.ir.getCallableReferenceTopLevelFlag
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.INT_TYPE
import org.jetbrains.org.objectweb.asm.Type.VOID_TYPE
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

class IrInlineIntrinsicsSupport(
    private val classCodegen: ClassCodegen,
    private val reportErrorsOn: IrElement,
    private val containingFile: IrFile,
) : ReifiedTypeInliner.IntrinsicsSupport<IrType> {
    override val config: JvmBackendConfig
        get() = classCodegen.context.config

    override fun putClassInstance(v: InstructionAdapter, type: IrType) {
        ExpressionCodegen.generateClassInstance(v, type, classCodegen.typeMapper, wrapPrimitives = false)
    }

    override fun generateTypeParameterContainer(v: InstructionAdapter, typeParameter: TypeParameterMarker) {
        require(typeParameter is IrTypeParameterSymbol)

        when (val parent = typeParameter.owner.parent) {
            is IrClass -> putClassInstance(v, parent.defaultType).also { AsmUtil.wrapJavaClassIntoKClass(v) }
            is IrSimpleFunction -> {
                val property = parent.correspondingPropertySymbol
                if (property != null) {
                    generatePropertyReference(v, property.owner)
                } else {
                    generateFunctionReference(v, parent)
                }
            }
            else -> error("Unknown parent of type parameter: ${parent.render()} ${typeParameter.owner.name})")
        }
    }

    private fun generateFunctionReference(v: InstructionAdapter, function: IrFunction) {
        generateCallableReference(v, function, function, FUNCTION_REFERENCE_IMPL, true)
    }

    private fun generatePropertyReference(v: InstructionAdapter, property: IrProperty) {
        // We're sure that this property has a getter because if a property is generic, it necessarily has extension receiver and
        // thus cannot have a backing field, and is required to have a getter.
        val getter = property.getter
            ?: error("Property without getter: ${property.render()}")
        val arity = getter.parameters.size
        val implClass = (if (property.isVar) MUTABLE_PROPERTY_REFERENCE_IMPL else PROPERTY_REFERENCE_IMPL).getOrNull(arity)
            ?: error("No property reference impl class with arity $arity (${property.render()}")

        generateCallableReference(v, property, getter, implClass, false)
    }

    private fun generateCallableReference(
        v: InstructionAdapter, declaration: IrDeclarationWithName, function: IrFunction, implClass: Type, withArity: Boolean
    ) {
        v.anew(implClass)
        v.dup()
        if (withArity) {
            v.iconst(function.parameters.size)
        }
        putClassInstance(v, declaration.parent.getCallableReferenceOwnerKClassType(classCodegen.context))
        v.aconst(declaration.name.asString())
        // TODO: generate correct signature for functions and property accessors which have inline class types in the signature.
        SignatureString.generateSignatureString(v, function, classCodegen)
        v.iconst(declaration.getCallableReferenceTopLevelFlag())
        val parameterTypes =
            (if (withArity) listOf(INT_TYPE) else emptyList()) +
                    listOf(JAVA_CLASS_TYPE, JAVA_STRING_TYPE, JAVA_STRING_TYPE, INT_TYPE)
        v.invokespecial(
            implClass.internalName, "<init>",
            Type.getMethodDescriptor(VOID_TYPE, *parameterTypes.toTypedArray()),
            false
        )
    }

    override fun isMutableCollectionType(type: IrType): Boolean {
        val classifier = type.classOrNull
        return classifier != null && JavaToKotlinClassMap.isMutable(classifier.owner.fqNameWhenAvailable?.toUnsafe())
    }

    override fun toKotlinType(type: IrType): KotlinType = type.toIrBasedKotlinType()

    override fun generateExternalEntriesForEnumTypeIfNeeded(type: IrType): FieldInsnNode? =
        generateExternalEntriesForEnumTypeIfNeeded(type, classCodegen)

    override fun reportSuspendTypeUnsupported() {
        classCodegen.context.ktDiagnosticReporter.at(reportErrorsOn, containingFile).report(JvmBackendErrors.TYPEOF_SUSPEND_TYPE)
    }

    override fun reportNonReifiedTypeParameterWithRecursiveBoundUnsupported(typeParameterName: Name) {
        classCodegen.context.ktDiagnosticReporter.at(reportErrorsOn, containingFile)
            .report(JvmBackendErrors.TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND, typeParameterName.asString())
    }

    override fun rewritePluginDefinedOperationMarker(
        v: InstructionAdapter,
        reifiedInsn: AbstractInsnNode,
        instructions: InsnList,
        type: IrType,
    ): Boolean {
        return classCodegen.intrinsicExtensions.any { it.rewritePluginDefinedOperationMarker(v, reifiedInsn, instructions, type) }
    }
}
