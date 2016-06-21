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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.types.KotlinType
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.memberProperties

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptFilePattern(val pattern: String)

interface ScriptDependenciesResolver {
    fun resolve(scriptFile: File?, annotations: Iterable<Annotation>, context: Any?): KotlinScriptExternalDependencies? = null
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptDependenciesResolverClass(val resolver: KClass<out ScriptDependenciesResolver>,
                                                 vararg val supportedAnnotationClasses: KClass<out Any>)

data class KotlinScriptDefinitionFromTemplate(val template: KClass<out Any>, val context: Any?) : KotlinScriptDefinition {
    override val name = template.simpleName!!

    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
            template.constructors.first().parameters.map { ScriptParameter(Name.identifier(it.name!!), getKotlinTypeByFqName(scriptDescriptor, it.type.toString())) }

    override fun getScriptSupertypes(scriptDescriptor: ScriptDescriptor): List<KotlinType> =
            listOf(getKotlinTypeByFqName(scriptDescriptor, template.qualifiedName!!))

    override fun getScriptParametersToPassToSuperclass(scriptDescriptor: ScriptDescriptor): List<Name> =
            getScriptParameters(scriptDescriptor).map { it.name }

    override fun <TF> isScript(file: TF): Boolean =
            template.annotations.any { (it as? ScriptFilePattern)?.let { Regex(it.pattern).matches(getFileName(file)) } ?: false }

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    private val dependenciesResolvers by lazy {
        template.annotations.mapNotNull { it as? ScriptDependenciesResolverClass }.map { Pair(it, it.resolver.constructors.first().call()) }
    }

    private val supportedAnnotationClasses: List<KClass<out Any>> by lazy {
        template.annotations.mapNotNull { it as? ScriptDependenciesResolverClass }
                .flatMap {
                    // it.supportedAnnotationClasses.asIterable() // TODO: find out why it fails with "java.lang.Class cannot be cast to kotlin.reflect.KClass"
                    it.supportedAnnotationClasses.asIterable2()
                }
                .distinctBy { it.qualifiedName }
    }

    private fun Array<out KClass<out Any>>.asIterable2(): ArrayList<KClass<out Any>> {
        val res = arrayListOf<KClass<out Any>>()
        for (x in this) {
            res.add(x)
        }
        return res
    }

    override fun <TF> getDependenciesFor(file: TF, project: Project): KotlinScriptExternalDependencies? {
        val fileAnnotations = getAnnotationEntries(file, project)
                .map { KtAnnotationWrapper(it) }
                .mapNotNull { wrappedAnn ->
                    // TODO: consider advanced matching using semantic similar to actual resolving
                    supportedAnnotationClasses.find { wrappedAnn.name == it.simpleName || wrappedAnn.name == it.qualifiedName }
                        ?.let { Pair(it, wrappedAnn) }
                }
        val annotations = fileAnnotations.map {
            try {
                val handler = AnnProxyInvocationHandler(it.first, it.second.valueArguments)
                val proxy = Proxy.newProxyInstance((template as Any).javaClass.classLoader, arrayOf(it.first.java), handler) as Annotation
                Pair(it.first, proxy)
            }
            catch (ex: Exception) {
                Pair(it.first, InvalidScriptResolverAnnotation(it.second.name, it.second.valueArguments, ex))
            }
        }
        val fileDeps = dependenciesResolvers.mapNotNull { resolver ->
            val supportedAnnotations = annotations.mapNotNull { ann ->
                val annFQN = ann.first.qualifiedName
                if (resolver.first.supportedAnnotationClasses.asIterable2().any { it.qualifiedName == annFQN }) ann.second else null
            }
            resolver.second.resolve(getFile(file), supportedAnnotations, context)
        }
        return KotlinScriptExternalDependenciesUnion(fileDeps)
    }

    private fun <TF> getAnnotationEntries(file: TF, project: Project): Iterable<KtAnnotationEntry> = when (file) {
        is PsiFile -> getAnnotationEntriesFromPsiFile(file)
        is VirtualFile -> getAnnotationEntriesFromVirtualFile(file, project)
        is File -> {
            val virtualFile = (StandardFileSystems.local().findFileByPath(file.canonicalPath)
                               ?: throw java.lang.IllegalArgumentException("Unable to find file ${file.canonicalPath}"))
            getAnnotationEntriesFromVirtualFile(virtualFile, project)
        }
        else -> throw IllegalArgumentException("Unsupported file type $file")
    }

    private fun getAnnotationEntriesFromPsiFile(file: PsiFile) =
            if (file is KtFile) file.annotationEntries
            else throw IllegalArgumentException("Unable to extract kotlin annotations from ${file.name} (${file.fileType})")

    private fun getAnnotationEntriesFromVirtualFile(file: VirtualFile, project: Project): Iterable<KtAnnotationEntry> {
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                               ?: throw java.lang.IllegalArgumentException("Unable to load PSI from ${file.canonicalPath}")
        return getAnnotationEntriesFromPsiFile(psiFile)
    }
}

@Suppress("unused")
class InvalidScriptResolverAnnotation(val name: String, val params: Iterable<Any?>, val error: Exception? = null) : Annotation

class AnnProxyInvocationHandler<out K: KClass<out Any>>(val targetAnnClass: K, val annParams: List<Pair<String?, Any?>>) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, params: Array<out Any>?): Any? {
        if (method == null) return null
        targetAnnClass.memberProperties.forEachIndexed { i, prop ->
            if (prop.name == method.name) {
                return if (i >= annParams.size) null else annParams[i].second
            }
        }
        return null
    }
}