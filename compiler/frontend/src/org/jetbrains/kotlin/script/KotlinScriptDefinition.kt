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

package org.jetbrains.kotlin.script

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

open class KotlinScriptDefinition(val template: KClass<out Any>) {

    open val name: String = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    open val fileType: LanguageFileType = KotlinFileType.INSTANCE

    open fun <TF> isScript(file: TF): Boolean =
            getFileName(file).endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    open fun getScriptName(script: KtScript): Name =
        ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    open fun <TF> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? = null
}

interface KotlinScriptExternalDependencies {
    val javaHome: String? get() = null
    val classpath: Iterable<File> get() = emptyList()
    val imports: Iterable<String> get() = emptyList()
    val sources: Iterable<File> get() = emptyList()
    val scripts: Iterable<File> get() = emptyList()
}

object StandardScriptDefinition : KotlinScriptDefinition(ScriptTemplateWithArgs::class)

