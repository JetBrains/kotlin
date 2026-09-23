/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Adds a source of the `kotlin.internal.ReflectionPackageName` annotation to every test module, for KLIB backward
 * compatibility tests whose first stage compiles against a stdlib older than 2.5, where the annotation does not exist yet.
 * `BatchingPackageInserter` annotates every file of a grouped test with it, so the first-stage compiler must resolve it.
 *
 * [ReflectionPackageNameHelperModuleTransformer] then moves the added sources into a dedicated helper module.
 */
class ReflectionPackageNameAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        // An isolated test keeps its original packages (see `BatchingPackageInserter`), so it never references the annotation.
        if (testServices.shouldIsolateTestInGroupingConfiguration(testModuleStructure, fileGenerationPhase = true)) return emptyList()

        val classLoader = this::class.java.classLoader
        return listOf(classLoader.getResource("klib/klib-compatibility/helpers/ReflectionPackageName.kt")!!.toTestFile())
    }
}
