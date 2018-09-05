/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.platform.impl

import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.JsAnalyzerFacade
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil
import org.jetbrains.kotlin.idea.highlighter.KotlinTestRunLineMarkerContributor.Companion.getTestStateIcon
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationDataProvider
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.idea.util.string.joinWithEscape
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.PathUtil
import javax.swing.Icon

class JsIdePlatformKindTooling : IdePlatformKindTooling() {
    companion object {
        private const val MAVEN_OLD_JS_STDLIB_ID = "kotlin-js-library"

        private val TEST_FQ_NAME = FqName("kotlin.test.Test")
        private val IGNORE_FQ_NAME = FqName("kotlin.test.Ignore")
    }

    override val kind = JsIdePlatformKind

    override fun compilerArgumentsForProject(project: Project) = Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings

    override val resolverForModuleFactory = JsAnalyzerFacade

    override val mavenLibraryIds = listOf(PathUtil.JS_LIB_NAME, MAVEN_OLD_JS_STDLIB_ID)
    override val gradlePluginId = "kotlin-platform-js"

    override val libraryKind = JSLibraryKind
    override fun getLibraryDescription(project: Project) = JSLibraryStdDescription(project)

    override fun getLibraryVersionProvider(project: Project) = { library: Library ->
        JsLibraryStdDetectionUtil.getJsLibraryStdVersion(library, project)
    }

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? {
        if (!descriptor.isTest()) return null

        val runConfigData = RunConfigurationProducer
            .getProducers(declaration.project)
            .asSequence()
            .filterIsInstance<KotlinJSRunConfigurationDataProvider<*>>()
            .filter { it.isForTests }
            .mapNotNull { it.getConfigurationData(declaration) }
            .firstOrNull() ?: return null

        val locations = ArrayList<String>()

        locations += FileUtil.toSystemDependentName(runConfigData.jsOutputFilePath)

        val klass = when (declaration) {
            is KtClassOrObject -> declaration
            is KtNamedFunction -> declaration.containingClassOrObject ?: return null
            else -> return null
        }
        locations += klass.parentsWithSelf.filterIsInstance<KtNamedDeclaration>().mapNotNull { it.name }.toList().asReversed()

        val testName = (declaration as? KtNamedFunction)?.name
        if (testName != null) {
            locations += "$testName"
        }

        val prefix = if (testName != null) "test://" else "suite://"

        val url = prefix + locations.joinWithEscape('.')

        return getTestStateIcon(url, declaration.project)
    }

    override fun acceptsAsEntryPoint(function: KtFunction): Boolean {
        return RunConfigurationProducer
            .getProducers(function.project)
            .asSequence()
            .filterIsInstance<KotlinJSRunConfigurationDataProvider<*>>()
            .filter { !it.isForTests }
            .mapNotNull { it.getConfigurationData(function) }
            .firstOrNull() != null
    }

    private fun DeclarationDescriptor.isIgnored(): Boolean =
        annotations.any { it.fqName == IGNORE_FQ_NAME } || ((containingDeclaration as? ClassDescriptor)?.isIgnored() ?: false)

    private fun DeclarationDescriptor.isTest(): Boolean {
        if (isIgnored()) return false

        if (annotations.any { it.fqName == TEST_FQ_NAME }) return true
        if (this is ClassDescriptorWithResolutionScopes) {
            return declaredCallableMembers.any { it.isTest() }
        }
        return false
    }
}