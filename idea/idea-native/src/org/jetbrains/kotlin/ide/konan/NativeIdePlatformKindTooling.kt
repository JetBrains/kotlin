/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import org.jetbrains.kotlin.ide.konan.analyzer.NativeAnalyzerFacade
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.idea.framework.CustomLibraryDescriptorWithDeferredConfig
import org.jetbrains.kotlin.idea.framework.KotlinLibraryKind
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.TargetPlatform
import javax.swing.Icon

class NativeIdePlatformKindTooling : IdePlatformKindTooling() {

    override val kind = NativeIdePlatformKind

    override fun compilerArgumentsForProject(project: Project): CommonCompilerArguments? = null

    override val resolverForModuleFactory get() = NativeAnalyzerFacade

    override val mavenLibraryIds: List<String> get() = emptyList()
    override val gradlePluginId: String get() = ""
    override val gradlePlatformIds: List<KotlinPlatform> get() = listOf(KotlinPlatform.NATIVE)

    override val libraryKind: PersistentLibraryKind<*> = NativeLibraryKind
    override fun getLibraryDescription(project: Project) = NativeStandardLibraryDescription(project)
    override fun getLibraryVersionProvider(project: Project): (Library) -> String? = { null }

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? = null

    override fun acceptsAsEntryPoint(function: KtFunction) = true
}

object NativeLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.native"), KotlinLibraryKind {
    override val compilerPlatform: TargetPlatform
        get() = NativeIdePlatformKind.compilerPlatform

    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}

class NativeStandardLibraryDescription(project: Project?) :
    CustomLibraryDescriptorWithDeferredConfig(
        project,
        KotlinNativeModuleConfigurator.NAME,
        LIBRARY_NAME,
        DIALOG_TITLE,
        LIBRARY_CAPTION,
        NativeLibraryKind,
        SUITABLE_LIBRARY_KINDS
    ) {

    companion object {
        val LIBRARY_NAME = "KotlinNative"

        val NATIVE_LIBRARY_CREATION = "Native Library Creation"
        val DIALOG_TITLE = "Create Kotlin Native Library"
        val LIBRARY_CAPTION = "Kotlin Native Library"
        val SUITABLE_LIBRARY_KINDS = setOf(NativeLibraryKind)
    }
}
