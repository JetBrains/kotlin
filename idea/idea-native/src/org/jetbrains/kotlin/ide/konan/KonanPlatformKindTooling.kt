/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import org.jetbrains.konan.analyser.KonanAnalyzerFacade
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import javax.swing.Icon

class KonanPlatformKindTooling : IdePlatformKindTooling {

    override val kind = KonanPlatformKind

    override fun compilerArgumentsForProject(project: Project): CommonCompilerArguments? = null

    override val resolverForModuleFactory get() = KonanAnalyzerFacade

    override val mavenLibraryIds: List<String> get() = TODO("not implemented")
    override val gradlePluginId: String get() = TODO("not implemented")

    override val libraryKind: PersistentLibraryKind<*>? = null
    override fun getLibraryDescription(project: Project) = TODO("not implemented")
    override fun getLibraryVersionProvider(project: Project) = TODO("not implemented")

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? = null

    override fun acceptsAsEntryPoint(function: KtFunction) = true
}
