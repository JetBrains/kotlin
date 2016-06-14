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

package org.jetbrains.kotlin.script

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.serialization.deserialization.findNonGenericClassAcrossDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import java.io.File
import kotlin.reflect.KClass

interface KotlinScriptDefinition {
    val name: String get() = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    val fileType: LanguageFileType get() = KotlinFileType.INSTANCE

    fun <TF> isScript(file: TF): Boolean =
            getFileName(file).endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    // TODO: replace these 3 functions with template property
    fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter>
    fun getScriptSupertypes(scriptDescriptor: ScriptDescriptor): List<KotlinType> = emptyList()
    fun getScriptParametersToPassToSuperclass(scriptDescriptor: ScriptDescriptor): List<Name> = emptyList()

    fun getScriptName(script: KtScript): Name =
        ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    fun <TF> getDependenciesFor(file: TF, project: Project): KotlinScriptExternalDependencies? = null
}

interface KotlinScriptExternalDependencies {
    val classpath: Iterable<File> get() = emptyList()
    val imports: Iterable<String> get() = emptyList()
    val sources: Iterable<File> get() = emptyList()
}

class KotlinScriptExternalDependenciesUnion(val dependencies: Iterable<KotlinScriptExternalDependencies>) : KotlinScriptExternalDependencies {
    override val classpath: Iterable<File> get() = dependencies.flatMap { it.classpath }
    override val imports: Iterable<String> get() = dependencies.flatMap { it.imports }
    override val sources: Iterable<File> get() = dependencies.flatMap { it.sources }
}

data class ScriptParameter(val name: Name, val type: KotlinType)

fun <TF> getFileName(file: TF): String = when (file) {
    is PsiFile -> file.originalFile.name
    is VirtualFile -> file.name
    is File -> file.name
    else -> throw IllegalArgumentException("Unsupported file type $file")
}

fun <TF> getFilePath(file: TF): String = when (file) {
    is PsiFile -> file.originalFile.run { virtualFile?.path ?: name } // TODO: replace name with path of PSI elements
    is VirtualFile -> file.path
    is File -> file.canonicalPath
    else -> throw IllegalArgumentException("Unsupported file type $file")
}

object StandardScriptDefinition : KotlinScriptDefinition {
    private val ARGS_NAME = Name.identifier("args")

    // NOTE: for now we treat .kts files as if they have 'args: Array<String>' parameter
    // this is not supposed to be final design
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
            makeStringListScriptParameters(scriptDescriptor, ARGS_NAME)
}

fun makeStringListScriptParameters(scriptDescriptor: ScriptDescriptor, propertyName: Name): List<ScriptParameter> {
    val builtIns = scriptDescriptor.builtIns
    val arrayOfStrings = builtIns.getArrayType(Variance.INVARIANT, builtIns.stringType)
    return listOf(ScriptParameter(propertyName, arrayOfStrings))
}

fun makeReflectedClassScriptParameter(scriptDescriptor: ScriptDescriptor, propertyName: Name, kClass: KClass<out Any>): ScriptParameter =
        ScriptParameter(propertyName, getKotlinType(scriptDescriptor, kClass))

fun getKotlinType(scriptDescriptor: ScriptDescriptor, kClass: KClass<out Any>): KotlinType =
        getKotlinTypeByFqName(scriptDescriptor,
                              kClass.qualifiedName ?: throw RuntimeException("Cannot get FQN from $kClass"))

fun getKotlinTypeByFqName(scriptDescriptor: ScriptDescriptor, fqName: String): KotlinType =
        scriptDescriptor.module.findNonGenericClassAcrossDependencies(
                ClassId.topLevel(FqName(fqName)),
                NotFoundClasses(LockBasedStorageManager.NO_LOCKS, scriptDescriptor.module)
        ).defaultType
