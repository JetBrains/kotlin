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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.org.objectweb.asm.Type

class InlineCodegenForDefaultBody(
        function: FunctionDescriptor,
        codegen: ExpressionCodegen,
        val state: GenerationState
) : CallGenerator() {

    private val functionDescriptor =
            if (InlineUtil.isArrayConstructorWithLambda(function))
                FictitiousArrayConstructor.create(function as ConstructorDescriptor)
            else
                function.original


    private val context = InlineCodegen.getContext(
            functionDescriptor, state,
            DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor)?.containingFile as? KtFile
    )

    private val jvmSignature = state.typeMapper.mapSignatureWithGeneric(functionDescriptor, context.contextKind)

    init {
        assert(InlineUtil.isInline(function)) {
            "InlineCodegen can inline only inline functions and array constructors: " + function
        }
        InlineCodegen.reportIncrementalInfo(functionDescriptor, codegen.context.functionDescriptor.original, jvmSignature, state)
    }

    override fun genCallInner(callableMethod: Callable, resolvedCall: ResolvedCall<*>?, callDefault: Boolean, codegen: ExpressionCodegen) {
        val nodeAndSmap = InlineCodegen.createMethodNode(functionDescriptor, jvmSignature, codegen, context, callDefault, state)
        nodeAndSmap.node.accept(MethodBodyVisitor(codegen.v))
    }

    override fun afterParameterPut(type: Type, stackValue: StackValue?, parameterIndex: Int) {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun genValueAndPut(valueParameterDescriptor: ValueParameterDescriptor, argumentExpression: KtExpression, parameterType: Type, parameterIndex: Int) {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun putValueIfNeeded(parameterType: Type, value: StackValue) {
        //original method would be inlined directly into default impl body without any inline magic
        //so we no need to load variables on stack to further method call
    }

    override fun putCapturedValueOnStack(stackValue: StackValue, valueType: Type, paramIndex: Int) {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun putHiddenParams() {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) {
        throw UnsupportedOperationException("Shouldn't be called")
    }
}