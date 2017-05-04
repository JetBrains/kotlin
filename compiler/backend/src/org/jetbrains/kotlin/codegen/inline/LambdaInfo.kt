/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.*
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import java.util.*

class LambdaInfo(
        expression: KtExpression,
        private val typeMapper: KotlinTypeMapper,
        @JvmField val isCrossInline: Boolean,
        val isBoundCallableReference: Boolean
) : LabelOwner {
    val functionWithBodyOrCallableReference: KtExpression = (expression as? KtLambdaExpression)?.functionLiteral ?: expression

    val labels: Set<String>
    private lateinit var closure: CalculatedClosure
    val functionDescriptor: FunctionDescriptor
    val classDescriptor: ClassDescriptor
    val lambdaClassType: Type

    var node: SMAPAndMethodNode? = null
    val propertyReferenceInfo: PropertyReferenceInfo?

    init {
        val bindingContext = typeMapper.bindingContext
        val function = bindingContext.get<PsiElement, SimpleFunctionDescriptor>(BindingContext.FUNCTION, this.functionWithBodyOrCallableReference)
        if (function == null && expression is KtCallableReferenceExpression) {
            val variableDescriptor = bindingContext.get<PsiElement, VariableDescriptor>(BindingContext.VARIABLE, this.functionWithBodyOrCallableReference)
            assert(variableDescriptor is VariableDescriptorWithAccessors) { "Reference expression not resolved to variable descriptor with accessors: " + expression.getText() }
            classDescriptor = CodegenBinding.anonymousClassForCallable(bindingContext, variableDescriptor!!)
            lambdaClassType = typeMapper.mapClass(classDescriptor)
            val getFunction = PropertyReferenceCodegen.findGetFunction(variableDescriptor)
            functionDescriptor = PropertyReferenceCodegen.createFakeOpenDescriptor(getFunction, classDescriptor)
            val resolvedCall = expression.callableReference.getResolvedCallWithAssert(bindingContext)
            propertyReferenceInfo = PropertyReferenceInfo(
                    resolvedCall.resultingDescriptor as VariableDescriptor, getFunction
            )
        }
        else {
            propertyReferenceInfo = null
            assert(function != null) { "Function is not resolved to descriptor: " + expression.text }
            functionDescriptor = function!!
            classDescriptor = anonymousClassForCallable(bindingContext, functionDescriptor)
            lambdaClassType = asmTypeForAnonymousClass(bindingContext, functionDescriptor)
        }

        bindingContext.get<ClassDescriptor, MutableClosure>(CLOSURE, classDescriptor).let {
            assert(it != null) { "Closure for lambda should be not null " + expression.text }
            closure = it!!
        }

        labels = InlineCodegen.getDeclarationLabels(expression, functionDescriptor)
    }

    val capturedVars: List<CapturedParamDesc> by lazy {
        arrayListOf<CapturedParamDesc>().apply {
            if (closure.captureThis != null) {
                val type = typeMapper.mapType(closure.captureThis!!)
                val descriptor = EnclosedValueDescriptor(
                        AsmUtil.CAPTURED_THIS_FIELD, null,
                        StackValue.field(type, lambdaClassType, AsmUtil.CAPTURED_THIS_FIELD, false, StackValue.LOCAL_0),
                        type
                )
                add(getCapturedParamInfo(descriptor))
            }

            if (closure.captureReceiverType != null) {
                val type = typeMapper.mapType(closure.captureReceiverType!!)
                val descriptor = EnclosedValueDescriptor(
                        AsmUtil.CAPTURED_RECEIVER_FIELD, null,
                        StackValue.field(type, lambdaClassType, AsmUtil.CAPTURED_RECEIVER_FIELD, false, StackValue.LOCAL_0),
                        type
                )
                add(getCapturedParamInfo(descriptor))
            }

            closure.captureVariables.values.forEach {
                descriptor -> add(getCapturedParamInfo(descriptor))
            }
        }
    }

    private fun getCapturedParamInfo(descriptor: EnclosedValueDescriptor): CapturedParamDesc {
        return CapturedParamDesc(lambdaClassType, descriptor.fieldName, descriptor.type)
    }

    val invokeParamsWithoutCaptured: List<Type>
        get() = Arrays.asList(*typeMapper.mapAsmMethod(functionDescriptor).argumentTypes)

    fun addAllParameters(remapper: FieldRemapper): Parameters {
        val asmMethod = typeMapper.mapAsmMethod(functionDescriptor)
        val builder = ParametersBuilder.initializeBuilderFrom(AsmTypes.OBJECT_TYPE, asmMethod.descriptor, this)

        for (info in capturedVars) {
            val field = remapper.findField(FieldInsnNode(0, info.containingLambdaName, info.fieldName, "")) ?: error("Captured field not found: " + info.containingLambdaName + "." + info.fieldName)
            builder.addCapturedParam(field, info.fieldName)
        }

        return builder.buildParameters()
    }

    override fun isMyLabel(name: String): Boolean {
        return labels.contains(name)
    }

    val isPropertyReference: Boolean
        get() = propertyReferenceInfo != null
}
