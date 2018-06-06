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

package org.jetbrains.kotlin.codegen

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.org.objectweb.asm.Type

class SamWrapperClasses(private val state: GenerationState) {

    private data class WrapperKey(val samType: SamType, val file: KtFile, val insideInline: Boolean)

    private val samInterfaceToWrapperClass = hashMapOf<WrapperKey, Type>()

    fun getSamWrapperClass(
        samType: SamType,
        file: KtFile,
        expressionCodegen: ExpressionCodegen,
        contextDescriptor: CallableMemberDescriptor
    ): Type {
        val isInsideInline = InlineUtil.isInlineOrContainingInline(expressionCodegen.context.contextDescriptor)
        return samInterfaceToWrapperClass.getOrPut(WrapperKey(samType, file, isInsideInline)) {
            SamWrapperCodegen(state, samType, expressionCodegen.parentCodegen, isInsideInline).genWrapper(file, contextDescriptor)
        }
    }
}
