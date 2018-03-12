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

package org.jetbrains.kotlin.codegen.context

import org.jetbrains.kotlin.codegen.FieldInfo
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.AsmTypes

class ScriptContext(
    typeMapper: KotlinTypeMapper,
    val scriptDescriptor: ScriptDescriptor,
    val earlierScripts: List<ScriptDescriptor>,
    contextDescriptor: ClassDescriptor,
    parentContext: CodegenContext<*>?
) : ClassContext(typeMapper, contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, null) {
    val lastStatement: KtExpression?

    val resultFieldInfo: FieldInfo
        get() {
            assert(state.replSpecific.shouldGenerateScriptResultValue) { "Should not be called unless 'scriptResultFieldName' is set" }
            val state = state
            val scriptResultFieldName = state.replSpecific.scriptResultFieldName!!
            return FieldInfo.createForHiddenField(state.typeMapper.mapClass(scriptDescriptor), AsmTypes.OBJECT_TYPE, scriptResultFieldName)
        }

    init {
        val script = DescriptorToSourceUtils.getSourceFromDescriptor(scriptDescriptor) as KtScript?
                ?: error("Declaration should be present for script: $scriptDescriptor")
        val lastDeclaration = script.declarations.lastOrNull()
        if (lastDeclaration is KtAnonymousInitializer) {
            this.lastStatement = lastDeclaration.body
        } else {
            this.lastStatement = null
        }
    }

    fun getScriptFieldName(scriptDescriptor: ScriptDescriptor): String {
        val index = earlierScripts.indexOf(scriptDescriptor)
        if (index < 0) {
            throw IllegalStateException("Unregistered script: $scriptDescriptor")
        }
        return "script$" + (index + 1)
    }

    override fun toString(): String {
        return "Script: " + contextDescriptor.name.asString()
    }
}
