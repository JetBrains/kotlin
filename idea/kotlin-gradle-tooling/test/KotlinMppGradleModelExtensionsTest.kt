/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalGradleToolingApi::class)
class KotlinMppGradleModelExtensionsTest {

    @Test
    fun resolveDeclaredDependsOnSourceSets() {
        val commonMain = createKotlinSourceSet("commonMain")
        val appleMain = createKotlinSourceSet("appleMain", declaredDependsOnSourceSets = setOf("commonMain"))
        val macosMain = createKotlinSourceSet("macosMain", declaredDependsOnSourceSets = setOf("appleMain"))
        val iosMain = createKotlinSourceSet("iosMain", declaredDependsOnSourceSets = setOf("appleMain", "commonMain"))

        val model = createKotlinMPPGradleModel(
            sourceSets = setOf(commonMain, appleMain, macosMain, iosMain)
        )

        assertEquals(
            emptySet(), model.resolveDeclaredDependsOnSourceSets(commonMain),
            "Expected no declared dependency source sets for commonMain"
        )

        assertEquals(
            setOf(commonMain), model.resolveDeclaredDependsOnSourceSets(appleMain),
            "Expected only declared dependency for 'appleMain'"
        )

        assertEquals(
            setOf(appleMain, commonMain), model.resolveDeclaredDependsOnSourceSets(iosMain),
            "Expected only declared dependency for 'iosMain'"
        )
    }

    @Test
    fun `resolveDeclaredDependsOnSourceSets is graceful for missing source sets`() {
        val commonMain = createKotlinSourceSet("commonMain")
        val macosMain = createKotlinSourceSet("macosMain", declaredDependsOnSourceSets = setOf("commonMain", "missing"))
        val model = createKotlinMPPGradleModel(sourceSets = setOf(commonMain, macosMain))

        assertEquals(
            setOf(commonMain), model.resolveDeclaredDependsOnSourceSets(macosMain),
            "Expected declaredDependencySourceSets to ignore missing dependency source set"
        )
    }

    @Test
    fun resolveAllDependsOnSourceSets() {
        val commonMain = createKotlinSourceSet("commonMain")
        val appleMain = createKotlinSourceSet("appleMain", declaredDependsOnSourceSets = setOf("commonMain"))
        val x64Main = createKotlinSourceSet("x64Main", declaredDependsOnSourceSets = setOf("commonMain"))
        val macosMain = createKotlinSourceSet("macosMain", declaredDependsOnSourceSets = setOf("appleMain"))
        val macosX64Main = createKotlinSourceSet("macosX64Main", declaredDependsOnSourceSets = setOf("appleMain", "x64Main"))
        val macosArm64Main = createKotlinSourceSet("macosArm64Main", declaredDependsOnSourceSets = setOf("appleMain"))

        val model = createKotlinMPPGradleModel(sourceSets = setOf(commonMain, appleMain, x64Main, macosMain, macosX64Main, macosArm64Main))

        assertEquals(
            setOf(appleMain, x64Main, commonMain),
            model.resolveAllDependsOnSourceSets(macosX64Main),
        )

        assertEquals(
            setOf(appleMain, x64Main, commonMain).sortedBy { it.name },
            model.resolveAllDependsOnSourceSets(macosX64Main).sortedBy { it.name }.toList(),
        )

        assertEquals(
            setOf(appleMain, commonMain),
            model.resolveAllDependsOnSourceSets(macosArm64Main).toSet(),
        )

        assertEquals(
            setOf(commonMain),
            model.resolveAllDependsOnSourceSets(appleMain).toSet(),
            "Expected only 'commonMain' for 'appleMain'"
        )

        assertEquals(
            emptySet(), model.resolveAllDependsOnSourceSets(commonMain).toSet(),
            "Expected empty set for 'commonMain'"
        )
    }

    @Test
    fun `resolving source sets with self references or loops`() {
        val a = createKotlinSourceSet("a", declaredDependsOnSourceSets = setOf(""))
        val b = createKotlinSourceSet("b", declaredDependsOnSourceSets = setOf("a", "c")) // loop d -> c -> b -> c
        val c = createKotlinSourceSet("c", declaredDependsOnSourceSets = setOf("b", "c")) // self reference c -> c
        val d = createKotlinSourceSet("d", declaredDependsOnSourceSets = setOf("c"))

        val model = createKotlinMPPGradleModel(sourceSets = setOf(a, b, c, d))

        assertEquals(
            setOf(a, b, c).sortedBy { it.name },
            model.resolveAllDependsOnSourceSets(d).sortedBy { it.name },
            "Expected dependency loop to be handled gracefully"
        )

        assertEquals(
            setOf(a, b, c).sortedBy { it.name },
            model.resolveAllDependsOnSourceSets(c).sortedBy { it.name },
            "Expected self dependency to be resolved for 'resolveAllDependsOnSourceSets'"
        )

        assertEquals(
            setOf(b, c).sortedBy { it.name },
            model.resolveDeclaredDependsOnSourceSets(c).sortedBy { it.name },
            "Expected self dependency to be resolved by 'resolveDeclaredDependsOnSourceSets'"
        )
    }

    @Test
    fun isDependsOn() {
        val commonMain = createKotlinSourceSet("commonMain")
        val appleMain = createKotlinSourceSet("appleMain", declaredDependsOnSourceSets = setOf("commonMain"))
        val x64Main = createKotlinSourceSet("x64Main", declaredDependsOnSourceSets = setOf("commonMain"))
        val macosX64Main = createKotlinSourceSet("macosX64Main", declaredDependsOnSourceSets = setOf("appleMain", "x64Main"))
        val macosArm64Main = createKotlinSourceSet("macosArm64Main", declaredDependsOnSourceSets = setOf("appleMain"))


        val model = createKotlinMPPGradleModel(sourceSets = setOf(commonMain, appleMain, x64Main, macosX64Main))

        assertTrue(
            model.isDependsOn(from = appleMain, to = commonMain),
            "Expected isDependsOn from appleMain to commonMain"
        )

        assertTrue(
            model.isDependsOn(from = x64Main, to = commonMain),
            "Expected isDependsOn from x64Main to commonMain"
        )

        assertTrue(
            model.isDependsOn(from = macosX64Main, to = commonMain),
            "Expected isDependsOn from macosX64Main to commonMain"
        )

        assertTrue(
            model.isDependsOn(from = macosX64Main, to = appleMain),
            "Expected isDependsOn from macosX64Main to appleMain"
        )

        assertTrue(
            model.isDependsOn(from = macosX64Main, to = commonMain),
            "Expected isDependsOn from macosX64Main to commonMain"
        )

        assertTrue(
            model.isDependsOn(from = macosX64Main, to = appleMain),
            "Expected isDependsOn from macosX64Main to appleMain"
        )

        assertTrue(
            model.isDependsOn(from = macosX64Main, to = x64Main),
            "Expected isDependsOn from macosX64Main to x64Main"
        )

        assertTrue(
            model.isDependsOn(from = macosArm64Main, to = commonMain),
            "Expected isDependsOn from macosArm64Main to commonMain"
        )

        assertTrue(
            model.isDependsOn(from = macosArm64Main, to = appleMain),
            "Expected isDependsOn from macosArm64Main to appleMain"
        )

        assertFalse(
            model.isDependsOn(from = macosArm64Main, to = x64Main),
            "Expected false isDependsOn from macosArm64 to x64Main"
        )

        for (sourceSet in model.sourceSetsByName.values) {
            assertFalse(
                model.isDependsOn(from = commonMain, to = sourceSet),
                "Expected false isDependsOn from commonMain to ${sourceSet.name}"
            )

            assertFalse(
                model.isDependsOn(from = sourceSet, to = sourceSet),
                "Expected false isDependsOn for same source set"
            )

            assertFalse(
                sourceSet.isDependsOn(model, sourceSet),
                "Expected false isDependsOn for same source set"
            )
        }
    }

    @Test
    fun compilationDependsOnSourceSet() {
        val commonMain = createKotlinSourceSet("commonMain")
        val appleMain = createKotlinSourceSet("appleMain", declaredDependsOnSourceSets = setOf("commonMain"))
        val macosMain = createKotlinSourceSet("macosMain", declaredDependsOnSourceSets = setOf("appleMain"))
        val iosMain = createKotlinSourceSet("iosMain", declaredDependsOnSourceSets = setOf("appleMain"))

        val metadataCompilation = createKotlinCompilation(defaultSourceSets = setOf(commonMain))
        val macosMainCompilation = createKotlinCompilation(defaultSourceSets = setOf(macosMain))
        val iosMainCompilation = createKotlinCompilation(defaultSourceSets = setOf(iosMain))

        val model = createKotlinMPPGradleModel(sourceSets = setOf(commonMain, appleMain, macosMain, iosMain))

        assertTrue(
            model.compilationDependsOnSourceSet(iosMainCompilation, iosMain),
            "Expected iosMainCompilation depending on iosMain"
        )

        assertTrue(
            model.compilationDependsOnSourceSet(iosMainCompilation, appleMain),
            "Expected iosMainCompilation depending on appleMain"
        )

        assertTrue(
            model.compilationDependsOnSourceSet(iosMainCompilation, commonMain),
            "Expected iosMainCompilation depending on commonMain"
        )

        assertFalse(
            model.compilationDependsOnSourceSet(iosMainCompilation, macosMain),
            "Expected iosMainCompilation *not* depending on macosMain"
        )

        assertTrue(
            model.compilationDependsOnSourceSet(macosMainCompilation, macosMain),
            "Expected macosMainCompilation depending on macosMain"
        )

        assertTrue(
            model.compilationDependsOnSourceSet(macosMainCompilation, appleMain),
            "Expected macosMainCompilation depending on appleMain"
        )

        assertTrue(
            model.compilationDependsOnSourceSet(macosMainCompilation, commonMain),
            "Expected macosMainCompilation depending on commonMain"
        )

        assertFalse(
            model.compilationDependsOnSourceSet(macosMainCompilation, iosMain),
            "Expected macosMainCompilation *not* depending on iosMain"
        )

        assertTrue(
            model.compilationDependsOnSourceSet(metadataCompilation, commonMain),
            "Expected metadataCompilation depending on commonMain"
        )

        assertFalse(
            model.compilationDependsOnSourceSet(metadataCompilation, appleMain),
            "Expected metadataCompilation *not* depending on appleMain"
        )

        assertFalse(
            model.compilationDependsOnSourceSet(metadataCompilation, macosMain),
            "Expected metadataCompilation *not* depending on macosMain"
        )

        assertFalse(
            model.compilationDependsOnSourceSet(metadataCompilation, iosMain),
            "Expected metadataCompilation *not* depending on iosMain"
        )
    }

    @Test
    fun getCompilations() {
        val commonMain = createKotlinSourceSet("commonMain")
        val appleMain = createKotlinSourceSet("appleMain", declaredDependsOnSourceSets = setOf("commonMain"))
        val macosMain = createKotlinSourceSet("macosMain", declaredDependsOnSourceSets = setOf("appleMain"))

        val metadataCompilation = createKotlinCompilation(defaultSourceSets = setOf(commonMain))
        val macosMainCompilation = createKotlinCompilation(defaultSourceSets = setOf(macosMain))

        val metadataTarget = createKotlinTarget("metadata", compilations = setOf(metadataCompilation))
        val macosTarget = createKotlinTarget("macos", compilations = setOf(macosMainCompilation))

        val model = createKotlinMPPGradleModel(
            sourceSets = setOf(commonMain, appleMain, macosMain),
            targets = setOf(metadataTarget, macosTarget)
        )

        assertEquals(
            setOf(metadataCompilation, macosMainCompilation),
            model.getCompilations(commonMain),
            "Expected correct compilations for commonMain"
        )

        assertEquals(
            setOf(macosMainCompilation), model.getCompilations(appleMain),
            "Expected correct compilations for appleMain"
        )

        assertEquals(
            setOf(macosMainCompilation), model.getCompilations(macosMain),
            "Expected correct compilations for macosMain"
        )
    }
}
