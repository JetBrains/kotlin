/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.platform.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.analyzer.common.CommonAnalyzerFacade
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.idea.caches.project.implementingModules
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.CommonStandardLibraryDescription
import org.jetbrains.kotlin.idea.framework.getCommonRuntimeLibraryVersion
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import javax.swing.Icon

object CommonIdePlatformKindTooling : IdePlatformKindTooling() {
    const val MAVEN_COMMON_STDLIB_ID = "kotlin-stdlib-common" // TODO: KotlinCommonMavenConfigurator

    override val kind = CommonIdePlatformKind

    override fun compilerArgumentsForProject(project: Project): CommonCompilerArguments? = null

    override val resolverForModuleFactory = CommonAnalyzerFacade

    override val mavenLibraryIds = listOf(MAVEN_COMMON_STDLIB_ID)
    override val gradlePluginId = "kotlin-platform-common"
    override val gradlePlatformIds: List<KotlinPlatform> get() = listOf(KotlinPlatform.COMMON)

    override val libraryKind = CommonLibraryKind

    override fun getLibraryDescription(project: Project) = CommonStandardLibraryDescription(project)

    override fun getLibraryVersionProvider(project: Project): (Library) -> String? {
        return ::getCommonRuntimeLibraryVersion
    }

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? {
        return IdePlatformKindTooling.getInstances()
            .asSequence()
            .filter { it != this }
            .mapNotNull { it.getTestIcon(declaration, descriptor) }
            .distinct()
            .singleOrNull()
    }

    override fun acceptsAsEntryPoint(function: KtFunction): Boolean {
        val module = function.containingKtFile.module ?: return false
        return module.implementingModules.any { implementingModule ->
            implementingModule.platform?.kind?.takeIf { !it.isCommon }?.tooling?.acceptsAsEntryPoint(function) ?: false
        }
    }
}