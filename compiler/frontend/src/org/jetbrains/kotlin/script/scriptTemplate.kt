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
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.*

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptTemplateDefinition(val resolver: KClass<out ScriptDependenciesResolver>,
                                          val scriptFilePattern: String)

interface ScriptDependenciesResolver {
    fun resolve(scriptFile: File?,
                annotations: Iterable<Annotation>,
                environment: Map<String, Any?>?,
                previousDependencies: KotlinScriptExternalDependencies? = null
    ): KotlinScriptExternalDependencies? = null
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcceptedAnnotations(vararg val supportedAnnotationClasses: KClass<out Annotation>)

data class KotlinScriptDefinitionFromTemplate(val template: KClass<out Any>, val environment: Map<String, Any?>?) : KotlinScriptDefinition {

    private val definitionAnnotation by lazy { template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>() }

    override val name = template.simpleName!!

    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
            template.primaryConstructor!!.parameters.map { ScriptParameter(Name.identifier(it.name!!), getKotlinTypeByFqName(scriptDescriptor, it.type.toString())) }

    override fun getScriptSupertypes(scriptDescriptor: ScriptDescriptor): List<KotlinType> =
            listOf(getKotlinTypeByFqName(scriptDescriptor, template.qualifiedName!!))

    override fun getScriptParametersToPassToSuperclass(scriptDescriptor: ScriptDescriptor): List<Name> =
            getScriptParameters(scriptDescriptor).map { it.name }

    override fun <TF> isScript(file: TF): Boolean =
            definitionAnnotation?.let { Regex(it.scriptFilePattern).matches(getFileName(file)) } ?: false

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    private class ResolverWithAcceptedAnnotations(val resolverClass: KClass<out ScriptDependenciesResolver>) {
        val resolver by lazy { resolverClass.primaryConstructor!!.call() }
        val acceptedAnnotations by lazy {
            val resolveMethod = ScriptDependenciesResolver::resolve
            val resolverMethodAnnotations =
                    resolverClass.memberFunctions.find {
                        it.name == resolveMethod.name &&
                        sameSignature(it, resolveMethod)
                    }
                    ?.annotations
                    ?.filterIsInstance<AcceptedAnnotations>()
            resolverMethodAnnotations?.flatMap {
                        val v = it.supportedAnnotationClasses
                        v.toList() // TODO: inline after KT-9453 is resolved (now it fails with "java.lang.Class cannot be cast to kotlin.reflect.KClass")
                    }
            ?: emptyList()
        }
    }

    private val dependenciesResolvers by lazy {
        definitionAnnotation?.resolver?.let { listOf(ResolverWithAcceptedAnnotations(it)) } ?: emptyList()
    }

    private val supportedAnnotationClasses: List<KClass<out Any>> by lazy {
        dependenciesResolvers.flatMap { it.acceptedAnnotations }
                .distinctBy { it.qualifiedName }
    }

    override fun <TF> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {
        val fileAnnotations = getAnnotationEntries(file, project)
                .map { KtAnnotationWrapper(it) }
                .mapNotNull { wrappedAnn ->
                    // TODO: consider advanced matching using semantic similar to actual resolving
                    supportedAnnotationClasses.find {
                        wrappedAnn.name == it.simpleName || wrappedAnn.name == it.qualifiedName
                    }?.let { it to wrappedAnn }
                }
        val annotations = fileAnnotations.map { annClassToWrapper ->
            try {
                val handler = AnnProxyInvocationHandler(annClassToWrapper.first, annClassToWrapper.second.valueArguments)
                val proxy = Proxy.newProxyInstance((template as Any).javaClass.classLoader, arrayOf(annClassToWrapper.first.java), handler) as Annotation
                annClassToWrapper.first to proxy
            }
            catch (ex: Exception) {
                annClassToWrapper.first to InvalidScriptResolverAnnotation(annClassToWrapper.second.name, annClassToWrapper.second.valueArguments, ex)
            }
        }
        val fileDeps = dependenciesResolvers.mapNotNull { resolverWrapper ->
            val supportedAnnotations = annotations.mapNotNull { annClassToWrapper ->
                val annFQN = annClassToWrapper.first.qualifiedName
                if (resolverWrapper.acceptedAnnotations.any { it.qualifiedName == annFQN }) annClassToWrapper.second else null
            }
            resolverWrapper.resolver.resolve(getFile(file), supportedAnnotations, environment, previousDependencies)
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

internal fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
        left.parameters.size == right.parameters.size &&
        left.parameters.zip(right.parameters).all {
            it.first.kind == KParameter.Kind.INSTANCE ||
            it.first.type == it.second.type
        }

