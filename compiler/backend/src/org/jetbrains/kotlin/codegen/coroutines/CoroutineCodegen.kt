/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.coroutines.controllerTypeIfCoroutine
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type


class CoroutineCodegen(
        state: GenerationState,
        element: KtElement,
        private val closureContext: ClosureContext,
        strategy: FunctionGenerationStrategy,
        parentCodegen: MemberCodegen<*>,
        classBuilder: ClassBuilder,
        private val continuationSuperType: KotlinType,
        private val controllerType: KotlinType
) : ClosureCodegen(state, element, null, closureContext, null, strategy, parentCodegen, classBuilder) {

    override fun generateClosureBody() {
        v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE,
                COROUTINE_CONTROLLER_FIELD_NAME,
                typeMapper.mapType(controllerType).descriptor, null, null)

        v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE,
                COROUTINE_LABEL_FIELD_NAME,
                Type.INT_TYPE.descriptor, null, null)

        val classDescriptor = closureContext.contextDescriptor

        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
            override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                AsmUtil.genAssignInstanceFieldFromParam(
                    FieldInfo.createForHiddenField(
                            typeMapper.mapClass(classDescriptor),
                            typeMapper.mapType(controllerType), COROUTINE_CONTROLLER_FIELD_NAME),
                    1, codegen.v)

                with(codegen.v) {
                    load(0, AsmTypes.OBJECT_TYPE)
                    iconst(0)
                    putfield(v.thisName, COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor)
                    load(0, AsmTypes.OBJECT_TYPE)
                    areturn(AsmTypes.OBJECT_TYPE)
                }
            }
        })

        val resumeFunctionDescriptor =
                createSynthesizedImplementationByName(
                        "resume",
                        interfaceSupertype = continuationSuperType,
                        implementationClass = classDescriptor,
                        sourceElement = funDescriptor.source)

        val resumeWithExceptionFunctionDescriptor =
                createSynthesizedImplementationByName(
                        "resumeWithException",
                        interfaceSupertype = continuationSuperType,
                        implementationClass = classDescriptor,
                        sourceElement = funDescriptor.source)

        // private fun resume(result, throwable)
        val combinedResumeFunctionDescriptor =
                resumeFunctionDescriptor.newCopyBuilder()
                        .setVisibility(Visibilities.PRIVATE)
                        .setName(Name.identifier("doResume"))
                        .setCopyOverrides(false)
                        .setPreserveSourceElement()
                        .setValueParameters(
                                listOf(
                                        resumeFunctionDescriptor.valueParameters[0].copy(
                                                resumeFunctionDescriptor, Name.identifier("value"), 0),
                                        resumeWithExceptionFunctionDescriptor.valueParameters[0].copy(
                                                resumeFunctionDescriptor, Name.identifier("exception"), 1))
                        ).build()!!

        generatedDelegationToCombinedResume(resumeFunctionDescriptor, combinedResumeFunctionDescriptor, isSuccess = true)
        generatedDelegationToCombinedResume(resumeWithExceptionFunctionDescriptor, combinedResumeFunctionDescriptor, isSuccess = false)

        functionCodegen.generateMethod(OtherOrigin(element), combinedResumeFunctionDescriptor,
                                       object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               codegen.v.visitAnnotation(CONTINUATION_METHOD_ANNOTATION_DESC, true).visitEnd()
                                               super.doGenerateBody(codegen, signature)
                                           }
                                       })
    }


    private fun createSynthesizedImplementationByName(
            name: String,
            interfaceSupertype: KotlinType,
            implementationClass: ClassDescriptor,
            sourceElement: SourceElement
    ): SimpleFunctionDescriptor {
        val inSuperType = interfaceSupertype.memberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).single()
        val result = inSuperType.newCopyBuilder().setSource(sourceElement).setOwner(implementationClass).setModality(Modality.FINAL).build()!!
        result.setSingleOverridden(inSuperType)

        return result
    }

    private fun generatedDelegationToCombinedResume(
            from: SimpleFunctionDescriptor,
            combinedResume: SimpleFunctionDescriptor,
            isSuccess: Boolean
    ) {
        functionCodegen.generateMethod(OtherOrigin(element), from,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               with(codegen.v) {
                                                   load(0, AsmTypes.OBJECT_TYPE)
                                                   if (isSuccess) {
                                                       load(1, AsmTypes.OBJECT_TYPE)
                                                       aconst(null)
                                                   }
                                                   else {
                                                       aconst(null)
                                                       load(1, AsmTypes.OBJECT_TYPE)
                                                   }

                                                   val delegateTo = typeMapper.mapAsmMethod(combinedResume)
                                                   invokevirtual(className, delegateTo.name, delegateTo.descriptor, false)
                                                   areturn(Type.VOID_TYPE)
                                               }
                                           }
                                       })
    }


    companion object {
        @JvmStatic fun create(
                expressionCodegen: ExpressionCodegen,
                originalCoroutineLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            val classDescriptor = expressionCodegen.bindingContext[CodegenBinding.CLASS_FOR_CALLABLE, originalCoroutineLambdaDescriptor] ?: return null
            declaration as? KtFunction ?: return null

            val continuationSupertype =
                    classDescriptor.typeConstructor.supertypes.firstOrNull {
                        it.constructor.declarationDescriptor?.fqNameUnsafe ==
                                DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME.toUnsafe()
                    } ?: return null

            val descriptorWithContinuationReturnType =
                    originalCoroutineLambdaDescriptor.newCopyBuilder().setPreserveSourceElement().setReturnType(continuationSupertype).build()!!

            val state = expressionCodegen.state
            return CoroutineCodegen(
                    state,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            descriptorWithContinuationReturnType, originalCoroutineLambdaDescriptor, expressionCodegen, state.typeMapper),
                    FunctionGenerationStrategy.FunctionDefault(state, declaration), expressionCodegen.parentCodegen, classBuilder,
                    continuationSupertype,
                    originalCoroutineLambdaDescriptor.controllerTypeIfCoroutine!!)
        }
    }
}
