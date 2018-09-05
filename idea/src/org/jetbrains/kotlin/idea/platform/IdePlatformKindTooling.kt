/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.platform

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import javax.swing.Icon

abstract class IdePlatformKindTooling {
    abstract val kind: IdePlatformKind<*>

    abstract fun compilerArgumentsForProject(project: Project): CommonCompilerArguments?

    abstract val resolverForModuleFactory: ResolverForModuleFactory

    abstract val mavenLibraryIds: List<String>
    abstract val gradlePluginId: String

    abstract val libraryKind: PersistentLibraryKind<*>?
    abstract fun getLibraryDescription(project: Project): CustomLibraryDescription
    abstract fun getLibraryVersionProvider(project: Project): (Library) -> String?

    abstract fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon?

    abstract fun acceptsAsEntryPoint(function: KtFunction): Boolean

    override fun equals(other: Any?): Boolean = javaClass == other?.javaClass
    override fun hashCode(): Int = javaClass.hashCode()

    companion object : ApplicationExtensionDescriptor<IdePlatformKindTooling>(
        "org.jetbrains.kotlin.idePlatformKindTooling", IdePlatformKindTooling::class.java
    ) {
        private val CACHED_TOOLING_SUPPORT by lazy {
            val allPlatformKinds = IdePlatformKind.ALL_KINDS
            val groupedTooling = IdePlatformKindTooling.getInstances().groupBy { it.kind }.mapValues { it.value.single() }

            for (kind in allPlatformKinds) {
                if (kind !in groupedTooling) {
                    throw IllegalStateException(
                        "Tooling support for the platform '$kind' is missing. " +
                                "Implement 'IdePlatformKindTooling' for it."
                    )
                }
            }

            groupedTooling
        }

        fun getTooling(kind: IdePlatformKind<*>): IdePlatformKindTooling {
            return CACHED_TOOLING_SUPPORT[kind] ?: error("Unknown platform $this")
        }
    }
}

val IdePlatformKind<*>.tooling: IdePlatformKindTooling
    get() = IdePlatformKindTooling.getTooling(this)