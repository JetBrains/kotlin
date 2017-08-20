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
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import kotlin.reflect.KClass
import kotlin.script.dependencies.KotlinScriptExternalDependencies
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.templates.standard.ScriptTemplateWithArgs

open class KotlinScriptDefinition(val template: KClass<out Any>) {

    open val name: String = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    open val fileType: LanguageFileType = KotlinFileType.INSTANCE

    open val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    open fun <TF : Any> isScript(file: TF): Boolean =
            getFileName(file).endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    open fun getScriptName(script: KtScript): Name =
            NameUtils.getScriptNameForFile(script.containingKtFile.name)

    @Deprecated("Use dependencyResolver instead", level = DeprecationLevel.ERROR)
    open fun <TF : Any> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? = null

    open val dependencyResolver: ScriptDependenciesResolver = EmptyDependencyResolver

    open val acceptedAnnotations: List<KClass<out Annotation>> = emptyList()

    private object EmptyDependencyResolver : ScriptDependenciesResolver
}

object StandardScriptDefinition : KotlinScriptDefinition(ScriptTemplateWithArgs::class)

