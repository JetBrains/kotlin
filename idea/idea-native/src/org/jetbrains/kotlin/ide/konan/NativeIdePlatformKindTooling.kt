/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.idea.caches.project.isTestModule
import org.jetbrains.kotlin.idea.facet.externalSystemNativeMainRunTasks
import org.jetbrains.kotlin.idea.framework.KotlinLibraryKind
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.idea.platform.getGenericTestIcon
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import javax.swing.Icon

class NativeIdePlatformKindTooling : IdePlatformKindTooling() {

    override val kind = NativeIdePlatformKind

    override fun compilerArgumentsForProject(project: Project): CommonCompilerArguments? = null

    override val mavenLibraryIds: List<String> get() = emptyList()
    override val gradlePluginId: String get() = ""
    override val gradlePlatformIds: List<KotlinPlatform> get() = listOf(KotlinPlatform.NATIVE)

    override val libraryKind: PersistentLibraryKind<*> = NativeLibraryKind
    override fun getLibraryDescription(project: Project): CustomLibraryDescription? = null
    override fun getLibraryVersionProvider(project: Project): (Library) -> String? = { null }

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? {
        return getGenericTestIcon(declaration, descriptor) {
            val availableRunConfigurations = RunConfigurationProducer
                .getProducers(declaration.project)
                .asSequence()
                .filterIsInstance<KotlinNativeRunConfigurationProvider>()
                .filter { it.isForTests }

            if (availableRunConfigurations.firstOrNull() == null) {
                return@getGenericTestIcon null
            }

            return@getGenericTestIcon emptyList()
        }
    }

    override fun acceptsAsEntryPoint(function: KtFunction): Boolean {
        if (!function.isMainFunction()) return false
        val functionName = function.fqName?.asString() ?: return false

        val module = function.module ?: return false
        if (module.isTestModule) return false

        val hasRunTask = module.externalSystemNativeMainRunTasks().any { it.entryPoint == functionName }
        if (!hasRunTask) return false

        val hasRunConfigurations = RunConfigurationProducer
            .getProducers(function.project)
            .asSequence()
            .filterIsInstance<KotlinNativeRunConfigurationProvider>()
            .any { !it.isForTests }
        if (!hasRunConfigurations) return false

        return true
    }
}

object NativeLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.native"), KotlinLibraryKind {
    override val compilerPlatform: TargetPlatform
        get() = NativePlatforms.unspecifiedNativePlatform

    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}
