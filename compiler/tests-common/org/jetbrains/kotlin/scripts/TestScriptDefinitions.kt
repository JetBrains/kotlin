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

package org.jetbrains.kotlin.scripts

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptNameUtil
import org.jetbrains.kotlin.script.ScriptParameter
import org.jetbrains.kotlin.script.makeReflectedClassScriptParameter
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass

abstract class BaseScriptDefinition (val extension: String) : KotlinScriptDefinition {
    override fun isScript(file: PsiFile): Boolean = file.name.endsWith(extension)
    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, extension)
}

class SimpleParamsTestScriptDefinition(extension: String, val parameters: List<ScriptParameter>) : BaseScriptDefinition(extension) {
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor) = parameters
}

class ReflectedParamClassTestScriptDefinition(extension: String, val name: String, val parameter: KClass<out Any>) : BaseScriptDefinition(extension) {
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor) =
            listOf(makeReflectedClassScriptParameter(scriptDescriptor, Name.identifier(name), parameter))
}
