/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

object KtResolveExtensionTestSupport {
    object Directives : SimpleDirectivesContainer() {
        val WITH_RESOLVE_EXTENSION by directive(
            "Enable resolve extension for this module",
            DirectiveApplicability.Module,
        )

        val RESOLVE_EXTENSION_PACKAGE by valueDirective(
            "Package name for resolve extension or resolve extension file",
            DirectiveApplicability.Any,
            ::FqName,
        )
        val RESOLVE_EXTENSION_SHADOWED by valueDirective(
            "File name regex(es) to shadow in resolve extension",
            DirectiveApplicability.Module,
            ::Regex,
        )

        val RESOLVE_EXTENSION_FILE by directive(
            "Treat this file as a KtResolveExtensionFile and remove it from the module",
            DirectiveApplicability.File,
        )
        val RESOLVE_EXTENSION_CLASSIFIER by stringDirective(
            "Name(s) of top-level classifier(s) in this resolve extension file",
            DirectiveApplicability.File,
        )
        val RESOLVE_EXTENSION_CALLABLE by stringDirective(
            "Names of top-level callables in this resolve extension file",
            DirectiveApplicability.File,
        )
    }

    @OptIn(TestInfrastructureInternals::class)
    private class ResolveExtensionDirectiveModuleStructureTransformer(
        private val testServices: TestServices,
    ) : ModuleStructureTransformer() {
        override fun transformModuleStructure(moduleStructure: TestModuleStructure): TestModuleStructure {
            check(Directives.WITH_RESOLVE_EXTENSION in moduleStructure.allDirectives) {
                "configureResolveExtensions() was called, but no modules specify WITH_RESOLVE_EXTENSION."
            }

            return TestModuleStructureImpl(
                moduleStructure.modules.map { it.transformAndRegisterResolveExtension() },
                moduleStructure.originalTestDataFiles,
            )
        }

        private fun TestModule.transformAndRegisterResolveExtension(): TestModule {
            val (resolveExtensionTestFiles, regularFiles) = files.partition { Directives.RESOLVE_EXTENSION_FILE in it.directives }
            if (Directives.WITH_RESOLVE_EXTENSION !in directives) {
                check(resolveExtensionTestFiles.isEmpty()) {
                    "Module $name has resolve extension files, but does not specify WITH_RESOLVE_EXTENSION"
                }
                return this
            }

            val packageNames = directives[Directives.RESOLVE_EXTENSION_PACKAGE].toSet()
            check(packageNames.isNotEmpty()) {
                 "Module $name does not specify any RESOLVE_EXTENSION_PACKAGE"
            }

            val shadowedPatterns = directives[Directives.RESOLVE_EXTENSION_SHADOWED]
            val shadowedScope = if (shadowedPatterns.isNotEmpty()) {
                object : GlobalSearchScope() {
                    override fun contains(file: VirtualFile): Boolean =
                        shadowedPatterns.any { it.containsMatchIn(file.name) }

                    override fun isSearchInModuleContent(module: Module): Boolean = true

                    override fun isSearchInLibraries(): Boolean = true
                }
            } else {
                GlobalSearchScope.EMPTY_SCOPE
            }

            val singleModulePackageName = packageNames.singleOrNull()
            val ktResolveExtensionFiles =
                resolveExtensionTestFiles.map { it.toKtResolveExtensionFile(singleModulePackageName) }

            val provider = KtResolveExtensionProviderForTest(ktResolveExtensionFiles, packageNames, shadowedScope) {
                it is KtSourceModule && it.moduleName == name
            }
            provider.register(testServices)

            return this.copy(files = regularFiles)
        }

        private fun TestFile.toKtResolveExtensionFile(moduleSinglePackageName: FqName?): KtResolveExtensionFile {
            val packageName = directives[Directives.RESOLVE_EXTENSION_PACKAGE].singleOrNull()
                ?: moduleSinglePackageName
                ?: error("Extension file $name must specify exactly one RESOLVE_EXTENSION_PACKAGE")
            val classifierNames = directives[Directives.RESOLVE_EXTENSION_CLASSIFIER].toSet()
            val callableNames = directives[Directives.RESOLVE_EXTENSION_CALLABLE].toSet()
            check(classifierNames.isNotEmpty() || callableNames.isNotEmpty()) {
                "Extension file $name does not specify any RESOLVE_EXTENSION_CLASSIFIER or RESOLVE_EXTENSION_CALLABLE"
            }

            return KtResolveExtensionFileForTests(
                fileName = name,
                packageName = packageName,
                topLevelClassifiersNames = classifierNames,
                topLevelCallableNames = callableNames,
                fileText = originalContent,
            )
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }
        useDirectives(Directives)
        useModuleStructureTransformers(KtResolveExtensionTestSupport::ResolveExtensionDirectiveModuleStructureTransformer)
    }
}