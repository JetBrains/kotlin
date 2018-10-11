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
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.location.ScriptExpectedLocation
import kotlin.script.templates.standard.ScriptTemplateWithArgs

open class KotlinScriptDefinition(open val template: KClass<out Any>) : UserDataHolderBase() {

    open val name: String = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    open val fileType: LanguageFileType = KotlinFileType.INSTANCE

    open val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    open fun isScript(fileName: String): Boolean =
        fileName.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    open fun getScriptName(script: KtScript): Name =
        NameUtils.getScriptNameForFile(script.containingKtFile.name)

    open val fileExtension: String
        get() = "kts"

    open val dependencyResolver: DependenciesResolver get() = DependenciesResolver.NoDependencies

    open val acceptedAnnotations: List<KClass<out Annotation>> get() = emptyList()

    @Deprecated("temporary workaround for missing functionality, will be replaced by the new API soon")
    open val additionalCompilerArguments: Iterable<String>? = null

    open val scriptExpectedLocations: List<ScriptExpectedLocation> =
        listOf(ScriptExpectedLocation.SourcesOnly, ScriptExpectedLocation.TestsOnly)

    open val implicitReceivers: List<KType> get() = emptyList()

    open val environmentVariables: List<Pair<String, KType>> get() = emptyList()

    open val targetClassAnnotations: List<Annotation> get() = emptyList()

    open val targetMethodAnnotations: List<Annotation> get() = emptyList()
}

object StandardScriptDefinition : KotlinScriptDefinition(ScriptTemplateWithArgs::class)

