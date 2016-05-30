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

import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KClass

interface KotlinScriptDefinition {
    fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter>
    fun isScript(file: PsiFile): Boolean
    fun getScriptName(script: KtScript): Name
}

class ScriptParameter(val name: Name, val type: KotlinType)

object StandardScriptDefinition : KotlinScriptDefinition {
    private val ARGS_NAME = Name.identifier("args")

    override fun getScriptName(script: KtScript): Name {
        return ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)
    }

    override fun isScript(file: PsiFile): Boolean {
        return PathUtil.getFileExtension(file.name) == KotlinParserDefinition.STD_SCRIPT_SUFFIX
    }

    // NOTE: for now we treat .kts files as if they have 'args: Array<String>' parameter
    // this is not supposed to be final design
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> {
        val kc: KClass<StandardScriptDefinition> = StandardScriptDefinition::class
        return makeStringListScriptParameters(scriptDescriptor, ARGS_NAME)
    }
}

fun makeStringListScriptParameters(scriptDescriptor: ScriptDescriptor, propertyName: Name): List<ScriptParameter> {
    val builtIns = scriptDescriptor.builtIns
    val arrayOfStrings = builtIns.getArrayType(Variance.INVARIANT, builtIns.stringType)
    return listOf(ScriptParameter(propertyName, arrayOfStrings))
}

fun makeReflectedClassScriptParameter(scriptDescriptor: ScriptDescriptor, propertyName: Name, kClass: KClass<out Any>): ScriptParameter =
        ScriptParameter(propertyName, getKotlinType(scriptDescriptor, kClass))

fun getKotlinType(scriptDescriptor: ScriptDescriptor, kClass: KClass<out Any>): KotlinType {
    val module = scriptDescriptor.module
    val qualifiedName = kClass.qualifiedName ?: throw RuntimeException("Cannot get FQN from $kClass")
    return module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(qualifiedName)))?.defaultType ?:
           throw RuntimeException("Cannot find class $qualifiedName in the dependencies, may be it is missing in the classpath")
}

