/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseDumpFileComparisonTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.junit.jupiter.api.Test

/**
 * This test was introduced to automatically check that every public API
 * from some [org.jetbrains.kotlin.analysis.api.components.KaSessionComponent] has a corresponding context parameter bridge in the same file.
 *
 * See KT-78093 Add bridges for context parameters
 *
 * The test iterates through all the source directories [sourceDirectories] and
 * for each directory [SourceDirectory.sourcePaths] builds a separate resulting file
 * containing all public API endpoints with no context parameter bridge along with fully qualified names and parameter types.
 *
 * Then the test compares the contents of the resulting file
 * and the master file [SourceDirectory.ForDumpFileComparison.outputFilePath]
 *
 * The test is intended to prevent developers from not implementing context parameter bridges,
 * as it's a vital feature for the users' experience.
 * If the lack of a context parameters bridge for some declaration is intentional,
 * the developer has to manually add this declaration to the master file.
 *
 * The test works as follows:
 * 1. For each file, the test finds all classes that are subtypes of [org.jetbrains.kotlin.analysis.api.components.KaSessionComponent] and collects all public members from them.
 * 2. In the exact same file it collects all top-level callable declarations that have [org.jetbrains.kotlin.analysis.api.KaContextParameterApi] annotation,
 * which marks them as context parameter bridges.
 * 3. For each member declaration from step 1, it checks if there's a corresponding context parameter bridge in the same file.
 * The test checks that the context parameter of the bridge matches the type of [org.jetbrains.kotlin.analysis.api.components.KaSessionComponent] the member belongs to
 * and then checks that their signatures are equivalent.
 * 4. If there's no corresponding context parameter bridge found, the test adds the member to the resulting file.
 *
 * The test also checks that there are no unused context parameter bridges, i.e., a bridge that doesn't have a pairing member declaration
 * and thus points to itself. If such a bridge is found, the test throws an exception.
 *
 * The test relies on two fundamental assumptions:
 * 1. All public children of [org.jetbrains.kotlin.analysis.api.components.KaSessionComponent] have it as their direct supertype.
 * 2. All context parameter bridges are annotated with [org.jetbrains.kotlin.analysis.api.KaContextParameterApi] and are located in the same file as the corresponding component.
 */
class AnalysisApiContextParametersBridgesTest : AbstractAnalysisApiCodebaseDumpFileComparisonTest() {
    @Test
    fun testContextParameterBridges() = doTest()

    override val sourceDirectories = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf("analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api"),
            "analysis/analysis-api/api/analysis-api.missing_bridges",
        )
    )

    override fun PsiFile.processFile(): List<String> = buildList {
        if (this@processFile !is KtFile) return emptyList()

        val membersBySessionComponents = this@processFile.getMembersByComponent()
        val bridgesByContextParameter = this@processFile.getBridgesByComponent()

        for ((sessionComponent, members) in membersBySessionComponents) {
            val relatedBridges = bridgesByContextParameter[sessionComponent] ?: emptyList()

            relatedBridges.filter { bridge ->
                members.none { member ->
                    bridge.hasTheSameSignatureWith(member)
                }
            }.ifNotEmpty {
                error(
                    "The following context parameters bridges are unused. Please, remove them:\n" +
                            this.joinToString("\n") { it.renderDeclaration() + " from " + it.containingKtFile.virtualFilePath }
                )
            }

            members.filter { member ->
                relatedBridges.none { bridge ->
                    bridge.hasTheSameSignatureWith(member)
                }
            }.forEach { memberWithNoBridge ->
                add(memberWithNoBridge.renderDeclaration())
            }
        }
    }

    override fun SourceDirectory.ForDumpFileComparison.getErrorMessage(): String =
        """
            The list of context parameter bridges `${getRoots()}` does not match the expected list in `$outputFilePath`.
            If you added new declarations to some `KaSessionComponent`, please implement a proper context parameter bridge for them.
            Otherwise, update the exclusion list accordingly.
        """.trimIndent()

    private fun KtFile.getMembersByComponent(): Map<String, List<KtCallableDeclaration>> =
        this.getSessionComponents()
            .associate { sessionComponent -> sessionComponent.name as String to sessionComponent.collectPublicMembers() }

    private fun KtFile.getBridgesByComponent(): Map<String, List<KtCallableDeclaration>> =
        this.collectPublicMembers().filter { callableDeclaration ->
            callableDeclaration.annotationEntries.any { annotation ->
                annotation.shortName.toString() == BRIDGE_ANNOTATION_MARKER
            }
        }.groupBy {
            it.modifierList?.contextReceiverList?.contextParameters()?.singleOrNull()?.typeReference?.typeElement?.text ?: "NO_COMPONENT"
        }

    private fun KtFile.getSessionComponents(): List<KtClass> {
        return this.declarations.filterIsInstance<KtClass>()
            .filter { ktClass ->
                ktClass.superTypeListEntries.any { superTypeListEntry ->
                    superTypeListEntry.text == KA_SESSION_COMPONENT
                } || ktClass.name == KA_SESSION_CLASS
            }
    }

    private fun KtElement.collectPublicMembers(): List<KtCallableDeclaration> =
        (this as KtDeclarationContainer).declarations.filterIsInstance<KtCallableDeclaration>().filter { it.isPublic }

    private fun KtCallableDeclaration.hasTheSameSignatureWith(other: KtCallableDeclaration): Boolean {
        if (this.name != other.name) return false
        if (this.typeReference?.text != other.typeReference?.text) return false
        if (this.receiverTypeReference?.text != other.receiverTypeReference?.text) return false
        if (this.valueParameters.map { it.name to it.typeReference?.text } !=
            other.valueParameters.map { it.name to it.typeReference?.text }) return false
        return true
    }

    companion object {
        private const val BRIDGE_ANNOTATION_MARKER = "KaContextParameterApi"
        private const val KA_SESSION_COMPONENT = "KaSessionComponent"
        private const val KA_SESSION_CLASS = "KaSession"
    }
}