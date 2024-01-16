/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.KmpModuleSorter
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.TestWithMockProject
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtSourceModuleImpl
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KmpModuleSorterTest : TestWithMockProject() {
    @Test
    fun testNonKmpDependencies() {
        val a = createKtModule("A")
        val b = createKtModule("B")
        val c = createKtModule("C")
        val d = createKtModule("D", directRegularDependencies = listOf(a, b, c))
        assertEquals(listOf(a, b, c), buildDependenciesToTest(d))
    }

    @Test
    fun testOnlyKmpDependencies() {
        val p1CommonMain = createKtModule("p1.commonMain")
        val p1NativeMain = createKtModule("p1.nativeMain", directDependsOnDependencies = listOf(p1CommonMain))
        val p1IosMain = createKtModule("p1.iosMain", directDependsOnDependencies = listOf(p1NativeMain))
        val p2IosMain = createKtModule("p2.iosMain", directRegularDependencies = listOf(p1CommonMain, p1NativeMain, p1IosMain))

        assertEquals(listOf(p1IosMain, p1NativeMain, p1CommonMain), buildDependenciesToTest(p2IosMain))
    }

    @Test
    fun testMixedKmpAndUsualDependenciesShuffled() {
        val a = createKtModule("A")
        val b1 = createKtModule("B1")
        val b2 = createKtModule("B2", directDependsOnDependencies = listOf(b1))
        val b3 = createKtModule("B3", directDependsOnDependencies = listOf(b2))
        val c = createKtModule("C")
        val d1 = createKtModule("D1")
        val d2 = createKtModule("D2", directDependsOnDependencies = listOf(d1))

        val p2IosMain = createKtModule(
            "p2.iosMain", directRegularDependencies = listOf(
                a, b2, c, b1, d2, b3, d1
            )
        )

        assertEquals(listOf(a, b3, c, b2, d2, b1, d1), this.buildDependenciesToTest(p2IosMain))
    }

    @Test
    fun testDependsOnDependenciesFromSelfAndOtherProject() {
        val p1Common = createKtModule("p1.common")
        val p1Intermediate = createKtModule("p1.intermediate", directDependsOnDependencies = listOf(p1Common))
        val p1Platform = createKtModule("p1.platform", directDependsOnDependencies = listOf(p1Intermediate))
        val p2Common = createKtModule("p2.common", directRegularDependencies = listOf(p1Common))
        val p2Intermediate = createKtModule(
            "p2.intermediate",
            directDependsOnDependencies = listOf(p2Common),
            directRegularDependencies = listOf(p1Common, p1Intermediate),
        )
        val p2Platform = createKtModule(
            "p2.platform",
            directDependsOnDependencies = listOf(p2Intermediate),
            directRegularDependencies = listOf(p1Common, p1Intermediate, p1Platform)
        )

        assertEquals(listOf(p1Common), this.buildDependenciesToTest(p2Common))
        assertEquals(listOf(p1Intermediate, p1Common, p2Common), this.buildDependenciesToTest(p2Intermediate))
        assertEquals(listOf(p1Platform, p1Intermediate, p1Common, p2Intermediate, p2Common), this.buildDependenciesToTest(p2Platform))
    }

    @Test
    fun testPartsOfTheGroupAreMergedCorrectly1() {
        val m1 = createKtModule("m1")
        val m2 = createKtModule("m2", directDependsOnDependencies = listOf(m1))
        val m3 = createKtModule("m3", directDependsOnDependencies = listOf(m2))
        val m4 = createKtModule("m4", directDependsOnDependencies = listOf(m3))

        val c = createKtModule("c", directRegularDependencies = listOf(m1, m3, m4, m2))
        val d = createKtModule("d", directRegularDependencies = listOf(m2, m4, m3, m1))
        assertEquals(listOf(m4, m3, m2, m1), buildDependenciesToTest(c))
        assertEquals(listOf(m4, m3, m2, m1), buildDependenciesToTest(d))
    }

    @Test
    fun testPartsOfTheGroupAreMergedCorrectly2() {
        val m1 = createKtModule("m1")
        val m2 = createKtModule("m2", directDependsOnDependencies = listOf(m1))
        val m3 = createKtModule("m3", directDependsOnDependencies = listOf(m2))
        val m4 = createKtModule("m4", directDependsOnDependencies = listOf(m3))
        val m5 = createKtModule("m5", directDependsOnDependencies = listOf(m4))
        val m6 = createKtModule("m6", directDependsOnDependencies = listOf(m5))

        val c = createKtModule("c", directRegularDependencies = listOf(m3, m4, m6, m5, m2, m1))
        val d = createKtModule("d", directRegularDependencies = listOf(m1, m2, m5, m6, m4, m3))
        assertEquals(listOf(m6, m5, m4, m3, m2, m1), buildDependenciesToTest(c))
        assertEquals(listOf(m6, m5, m4, m3, m2, m1), buildDependenciesToTest(d))
    }

    // See [org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirAbstractSessionFactory#collectDependencySymbolProviders]
    private fun buildDependenciesToTest(module: KtModule): List<KtModule> {
        val dependenciesToSort = buildSet {
            addAll(module.directRegularDependencies)
            addAll(module.transitiveDependsOnDependencies)
        }
        return KmpModuleSorter.order(dependenciesToSort.toList())
    }

    private fun createKtModule(
        name: String,
        directRegularDependencies: List<KtModule> = emptyList(),
        directDependsOnDependencies: List<KtModule> = emptyList(),
        directFriendDependencies: List<KtModule> = emptyList(),
    ): KtModule {
        return KtSourceModuleImpl(
            name, CommonPlatforms.defaultCommonPlatform, LanguageVersionSettingsImpl.DEFAULT, project, GlobalSearchScope.EMPTY_SCOPE
        ).apply {
            this.directRegularDependencies.addAll(directRegularDependencies)
            this.directFriendDependencies.addAll(directFriendDependencies)
            this.directDependsOnDependencies.addAll(directDependsOnDependencies)
        }
    }
}
