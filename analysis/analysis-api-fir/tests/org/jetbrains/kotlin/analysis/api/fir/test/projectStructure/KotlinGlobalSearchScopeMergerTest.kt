/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.TestServices
import org. junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * This test checks specific scenarios which cannot be covered with the project structure tests in
 * [org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractContentAndResolutionScopesProvidersTest].
 *
 * The test doesn't need any source code, so it operates on an empty test data file.
 */
class KotlinGlobalSearchScopeMergerTest : AbstractAnalysisApiExecutionTest("testData/projectStructure/globalSearchScopeMerger") {
    override val configurator: AnalysisApiTestConfigurator = LLSourceLikeTestConfigurator()

    private class UniqueScope : GlobalSearchScope() {
        override fun isSearchInModuleContent(aModule: Module): Boolean = false
        override fun isSearchInLibraries(): Boolean = false
        override fun contains(file: VirtualFile): Boolean = false
    }

    @Test
    @TestMetadata("empty.kt")
    fun `non-mergeable intersection scopes don't cause a stack overflow`(testServices: TestServices) {
        val scope1 = UniqueScope()
        val scope2 = UniqueScope()
        val scope3 = UniqueScope()
        val scope4 = UniqueScope()

        val intersectionScope1 = scope1.intersectWith(scope2)
        val intersectionScope2 = scope3.intersectWith(scope4)

        val project = testServices.environmentManager.getProject()
        val mergedScope = KaGlobalSearchScopeMerger.getInstance(project).union(listOf(intersectionScope1, intersectionScope2))

        assertEquals(
            setOf(intersectionScope1, intersectionScope2),
            GlobalSearchScopeUtil.flattenUnionScope(mergedScope).toSet(),
            "The merged scope should contain the same intersection scopes."
        )
    }
}
