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

package org.jetbrains.kotlin.cli.jvm.repl.codeinsight

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cli.common.repl.ReplClassLoader
import org.jetbrains.kotlin.cli.jvm.repl.ReplCodeAnalyzer
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.resolve.ResolutionFacadeBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy.getProxyClass

interface ReplCodeInsightFacade {
    fun complete(expression: KtSimpleNameExpression, identifierPart: String): Collection<DeclarationDescriptor>
}

class ReplCodeInsightFacadeImpl(replCodeAnalyzer: ReplCodeAnalyzer) : ReplCodeInsightFacade {
    companion object {
        private val CALLABLES_AND_CLASSIFIERS = DescriptorKindFilter(
                DescriptorKindFilter.CALLABLES_MASK or DescriptorKindFilter.CLASSIFIERS_MASK)

        fun create(replCodeAnalyzer: ReplCodeAnalyzer): ReplCodeInsightFacade {
            val kotlinCodeInsightFile = getKotlinCodeInsightJar()
            val classLoaderWithCodeInsight = ReplClassLoader(listOf(kotlinCodeInsightFile), replCodeAnalyzer.javaClass.classLoader)

            fun addClass(className: String) {
                val bytes = ReplCodeInsightFacadeImpl::class.java.classLoader
                        .getResource(className.replace('.', '/') + ".class").readBytes()

                classLoaderWithCodeInsight.addClass(JvmClassName.byFqNameWithoutInnerClasses(className), bytes)
            }

            addClass(ReplCodeInsightFacadeImpl::class.java.name)
            addClass("org.jetbrains.kotlin.cli.jvm.repl.codeinsight.ResolutionFacadeForRepl")

            val actualImplementation = Class.forName(ReplCodeInsightFacadeImpl::class.java.name, true, classLoaderWithCodeInsight)
                    .declaredConstructors.single().newInstance(replCodeAnalyzer)

            val proxyClass = getProxyClass(ReplCodeInsightFacadeImpl::class.java.classLoader, ReplCodeInsightFacade::class.java)
            val proxy = proxyClass.constructors.single().newInstance(InvocationHandler { _, method, args ->
                val isStatic = Modifier.isStatic(method.modifiers)

                fun parameterTypesMatches(first: Array<Class<*>>, second: Array<Class<*>>): Boolean {
                    if (first.size != second.size) return false
                    return first.indices.none { first[it] != second[it] }
                }

                actualImplementation.javaClass.methods
                        .single { it.name == method.name && parameterTypesMatches(it.parameterTypes, method.parameterTypes) }
                        .invoke(if (isStatic) null else actualImplementation, *(args ?: emptyArray()))
            })

            return proxy as ReplCodeInsightFacade
        }

        private fun getKotlinCodeInsightJar(): File {
            System.getProperty("kotlin.code.insight.jar")?.let { return File(it) }

            val kotlinCompilerJarFile = getClasspathEntry(ReplCodeAnalyzer::class.java) ?: error("Can't find Kotlin compiler")

            val kotlinCodeInsightFile = File(kotlinCompilerJarFile.parentFile, "kotlin-code-insight.jar")
            if (!kotlinCodeInsightFile.exists()) {
                error("Can't find kotlin-code-insight.jar")
            }

            return kotlinCodeInsightFile
        }

        private fun getClasspathEntry(clazz: Class<*>): File? {
            val classFileName = clazz.name.replace('.', '/') + ".class"
            val resource = clazz.classLoader.getResource(classFileName) ?: return null

            return File(
                when (resource.protocol?.toLowerCase()) {
                    "file" -> resource.path
                        ?.takeIf { it.endsWith(classFileName) }
                        ?.removeSuffix(classFileName)
                    "jar" -> resource.path
                        .takeIf { it.startsWith("file:", ignoreCase = true) }
                        ?.drop("file:".length)
                        ?.substringBefore("!/")
                    else -> null
                }
            )
        }
    }

    private val referenceVariantsHelper = ReferenceVariantsHelper(
            replCodeAnalyzer.trace.bindingContext,
            ResolutionFacadeForRepl(replCodeAnalyzer),
            replCodeAnalyzer.module)

    override fun complete(expression: KtSimpleNameExpression, identifierPart: String): Collection<DeclarationDescriptor> {
        // TODO do not filter out
        return referenceVariantsHelper.getReferenceVariants(expression, CALLABLES_AND_CLASSIFIERS, NameFilter(identifierPart))
    }

    class NameFilter(val prefix: String) : (Name) -> Boolean {
        override fun invoke(name: Name): Boolean {
            return !name.isSpecial && applicableNameFor(prefix, name.identifier)
        }

        private fun applicableNameFor(prefix: String, completion: String): Boolean {
            return completion.startsWith(prefix) || completion.toLowerCase().startsWith(prefix)
        }
    }
}

class ResolutionFacadeForRepl(private val replCodeAnalyzer: ReplCodeAnalyzer) : ResolutionFacadeBase {
    override val project: Project
    get() = replCodeAnalyzer.project

    override val moduleDescriptor: ModuleDescriptor
    get() = replCodeAnalyzer.module

    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        return replCodeAnalyzer.componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw NotImplementedError()
    }

    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        throw NotImplementedError()
    }

    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        throw NotImplementedError()
    }
}