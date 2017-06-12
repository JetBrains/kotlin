/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.org.objectweb.asm.Type

class IrInlineCodegen(
        codegen: ExpressionCodegen,
        state: GenerationState,
        function: FunctionDescriptor,
        typeParameterMappings: TypeParameterMappings,
        sourceCompiler: SourceCompilerForInline
) : InlineCodegen<ExpressionCodegen>(codegen, state, function, typeParameterMappings, sourceCompiler), IrCallGenerator {

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun genValueAndPut(valueParameterDescriptor: ValueParameterDescriptor?, argumentExpression: IrExpression, parameterType: Type, parameterIndex: Int, codegen: ExpressionCodegen, blockInfo: BlockInfo) {
        if (argumentExpression is IrBlock && argumentExpression.origin == IrStatementOrigin.LAMBDA) {
            //TODO
        }
        super.genValueAndPut(valueParameterDescriptor, argumentExpression, parameterType, parameterIndex, codegen, blockInfo)
    }
}