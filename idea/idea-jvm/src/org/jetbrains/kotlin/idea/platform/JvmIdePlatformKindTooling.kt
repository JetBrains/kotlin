/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.platform.impl

import com.intellij.codeInsight.TestFrameworks
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.framework.JavaRuntimeDetectionUtil
import org.jetbrains.kotlin.idea.framework.JavaRuntimeLibraryDescription
import org.jetbrains.kotlin.idea.highlighter.KotlinTestRunLineMarkerContributor.Companion.getTestStateIcon
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.utils.PathUtil
import javax.swing.Icon

class JvmIdePlatformKindTooling : IdePlatformKindTooling() {
    override val kind = JvmIdePlatformKind

    override fun compilerArgumentsForProject(project: Project) = Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings

    override val resolverForModuleFactory = JvmAnalyzerFacade

    override val mavenLibraryIds = listOf(
        PathUtil.KOTLIN_JAVA_STDLIB_NAME,
        PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_NAME,
        PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_NAME,
        PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_NAME,
        PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_NAME
    )

    override val gradlePluginId = "kotlin-platform-jvm"

    override val libraryKind: PersistentLibraryKind<*>? = null
    override fun getLibraryDescription(project: Project) = JavaRuntimeLibraryDescription(project)

    override fun getLibraryVersionProvider(project: Project): (Library) -> String? {
        return JavaRuntimeDetectionUtil::getJavaRuntimeVersion
    }

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? {
        val (url, framework) = when (declaration) {
            is KtClassOrObject -> {
                val lightClass = declaration.toLightClass() ?: return null
                val framework = TestFrameworks.detectFramework(lightClass) ?: return null
                if (!framework.isTestClass(lightClass)) return null
                val qualifiedName = lightClass.qualifiedName ?: return null

                "java:suite://$qualifiedName" to framework
            }

            is KtNamedFunction -> {
                val lightMethod = declaration.toLightMethods().firstOrNull() ?: return null
                val lightClass = lightMethod.containingClass as? KtLightClass ?: return null
                val framework = TestFrameworks.detectFramework(lightClass) ?: return null
                if (!framework.isTestMethod(lightMethod)) return null

                "java:test://${lightClass.qualifiedName}.${lightMethod.name}" to framework
            }

            else -> return null
        }
        return getTestStateIcon(url, declaration.project) ?: framework.icon
    }

    override fun acceptsAsEntryPoint(function: KtFunction) = true
}