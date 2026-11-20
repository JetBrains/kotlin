/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.cli.common.allowNoSourceFiles
import org.jetbrains.kotlin.cli.common.moduleChunk
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

@OptIn(SymbolInternals::class)
class AdditionalBuiltInsMembersSignatureListsTest {
    private val testRootDisposable: Disposable = Disposer.newDisposable()

    @AfterEach
    fun tearDown() {
        Disposer.dispose(testRootDisposable)
    }

    @Test
    fun testAllListedSignaturesExistInJdk() {
        val pipelineArtifact = JvmFrontendPipelinePhase.executePhase(
            ConfigurationPipelineArtifact(createCompilerConfiguration(), testRootDisposable)
        ) ?: fail("Failed to execute ${JvmFrontendPipelinePhase.name}")

        val moduleOutput = pipelineArtifact.frontendOutput.outputs.single()
        val session = moduleOutput.session
        val scopeSession = moduleOutput.scopeSession

        val hiddenSignatures =
            JvmBuiltInsSignatures.HIDDEN_METHOD_SIGNATURES +
                    JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES +
                    JvmBuiltInsSignatures.HIDDEN_CONSTRUCTOR_SIGNATURES +
                    JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES +
                    JvmBuiltInsSignatures.VISIBLE_CONSTRUCTOR_SIGNATURES

        val groupedByInternalName = hiddenSignatures.groupBy({ it.split(".")[0] }) { it.split(".")[1] }

        groupedByInternalName.entries.forEach { it ->
            val [internalName, jvmDescriptors] = it

            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(internalName.toTopLevelClassId())
                as? FirRegularClassSymbol
                ?: fail("Expected class with internal name $internalName to be resolved")

            val classScope = classSymbol.fir.unsubstitutedScope(
                session,
                scopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = null,
            )

            val signaturesFromLaterJdkReleases = SIGNATURES_FROM_LATER_JDK_RELEASES[internalName] ?: emptySet()

            for (jvmDescriptor in jvmDescriptors) {
                if (jvmDescriptor in signaturesFromLaterJdkReleases) continue

                val stringName = jvmDescriptor.split("(")[0]
                val functions = if (stringName == "<init>") {
                    classScope.getDeclaredConstructors()
                } else {
                    classScope.getFunctions(Name.identifier(stringName)).filter {
                        !it.isSubstitutionOrIntersectionOverride && classSymbol.toLookupTag().isRealOwnerOf(it)
                    }
                }

                val matchingFunction = functions.singleOrNull {
                    it.effectiveVisibility.publicApi && it.fir.computeJvmDescriptor() == jvmDescriptor
                }
                if (matchingFunction == null) fail("Expected single function with signature $jvmDescriptor in $internalName")
            }
        }
    }

    private fun createCompilerConfiguration(): CompilerConfiguration {
        return KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK_21, KtTestUtil.getAnnotationsJar())
            .apply {
                allowNoSourceFiles = true
                moduleChunk = ModuleChunk(listOf(ModuleBuilder("test-module", ".", "java-production")))
            }
    }

    private fun String.toTopLevelClassId(): ClassId = ClassId.topLevel(FqName(replace('/', '.')))

    /**
     * This property can be used to exclude particular signatures
     * from the [testAllListedSignaturesExistInJdk] test
     * if they were introduced in later JDK releases than the test is currently configured to use.
     * It might be helpful in case the test can't be updated to these later JDK releases just yet
     * (for example, if the JDK releases in question are not released as stable yet).
     *
     * initialization example: `mapOf("java/util/List" to setOf("addFirst(Ljava/lang/Object;)V"))`
     */
    private val SIGNATURES_FROM_LATER_JDK_RELEASES: Map<String, Set<String>> = emptyMap()
}
