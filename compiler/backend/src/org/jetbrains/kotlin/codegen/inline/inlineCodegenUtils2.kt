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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState

//This method was moved to separate class cause of LightClass generation problem: KT-18419
//Move it back to inlineCodegenUtil after fix
fun initDefaultSourceMappingIfNeeded(
    context: CodegenContext<*>, codegen: MemberCodegen<*>, state: GenerationState
) {
    if (state.isInlineDisabled) return

    var parentContext: CodegenContext<*>? = context.parentContext
    while (parentContext != null) {
        if (parentContext.isInlineMethodContext) {
            //just init default one to one mapping
            codegen.orCreateSourceMapper
            break
        }
        parentContext = parentContext.parentContext
    }
}
