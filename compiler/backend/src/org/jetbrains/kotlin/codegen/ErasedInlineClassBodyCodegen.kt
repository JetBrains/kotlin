/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.resolve.jvm.diagnostics.Synthetic
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

class ErasedInlineClassBodyCodegen(
    aClass: KtClass,
    context: ClassContext,
    v: ClassBuilder,
    state: GenerationState,
    parentCodegen: MemberCodegen<*>?
) : ClassBodyCodegen(aClass, context, v, state, parentCodegen) {

    private val superClassAsmType: Type by lazy {
        SuperClassInfo.getSuperClassInfo(descriptor, typeMapper).type
    }

    private val classAsmType = typeMapper.mapErasedInlineClass(descriptor)

    private val constructorCodegen = ConstructorCodegen(
        descriptor, context, functionCodegen, this, this, state, kind, v, classAsmType, aClass, bindingContext
    )

    override fun generateDeclaration() {
        v.defineClass(
            myClass.psiOrParent, state.classFileVersion, Opcodes.ACC_FINAL or Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC,
            classAsmType.internalName, null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY
        )
        v.visitSource(myClass.containingKtFile.name, null)
    }

    override fun generateConstructors() {
        val delegationFieldsInfo = DelegationFieldsInfo(classAsmType, descriptor, state, bindingContext)

        constructorCodegen.generatePrimaryConstructor(
            delegationFieldsInfo.getDelegationFieldsInfo(myClass.superTypeListEntries), superClassAsmType
        )

        for (secondaryConstructor in descriptor.secondaryConstructors) {
            constructorCodegen.generateSecondaryConstructor(secondaryConstructor, superClassAsmType)
        }
    }

    override fun generateSyntheticPartsAfterBody() {
        super.generateSyntheticPartsAfterBody()

        generateUnboxMethod()
    }

    private fun generateUnboxMethod() {
        val boxMethodDescriptor = InlineClassDescriptorResolver.createBoxFunctionDescriptor(descriptor) ?: return

        functionCodegen.generateMethod(
            Synthetic(null, boxMethodDescriptor), boxMethodDescriptor, object : FunctionGenerationStrategy.CodegenBased(state) {
                override fun mapMethodSignature(
                    functionDescriptor: FunctionDescriptor,
                    typeMapper: KotlinTypeMapper,
                    contextKind: OwnerKind,
                    hasSpecialBridge: Boolean
                ): JvmMethodGenericSignature {
                    // note that in signatures of functions inside erased inline class only underlying types are using,
                    // but return type of `box` function should be a wrapper type, therefore we handle this situation here differently
                    return typeMapper.mapSignatureForBoxMethodOfInlineClass(functionDescriptor)
                }

                override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                    val iv = codegen.v
                    val wrapperType = signature.returnType
                    val baseValueType = signature.valueParameters.single().asmType
                    val constructor = typeMapper.mapToCallableMethod(descriptor.unsubstitutedPrimaryConstructor!!, false)
                    iv.anew(wrapperType)
                    iv.dup()
                    iv.load(0, baseValueType)
                    constructor.genInvokeInstruction(iv)
                    iv.areturn(wrapperType)
                }
            }
        )
    }

    override fun generateKotlinMetadataAnnotation() {
        writeSyntheticClassMetadata(v, state)
    }
}