/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.model.DefaultTargetBackendComputer
import org.jetbrains.kotlin.generators.model.TargetBackendComputer
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.runners.RunnerWithTargetBackendForTestGeneratorMarker
import java.lang.reflect.Modifier

object ReflectionBasedTargetBackendComputer : TargetBackendComputer {
    private val runnerMarkerKClass = RunnerWithTargetBackendForTestGeneratorMarker::class.java

    override fun compute(definedTargetBackend: TargetBackend?, testKClass: Class<*>): TargetBackend {
        if (!runnerMarkerKClass.isAssignableFrom(testKClass)) return DefaultTargetBackendComputer.compute(definedTargetBackend, testKClass)
        require(definedTargetBackend == null) {
            """
                Test ${testKClass.simpleName} is inheritor of ${runnerMarkerKClass.simpleName} which means that
                   target you should not specify targetBackend in test generation DSL, because it will be 
                   read from test runner class itself
            """.trimIndent()
        }
        require(!Modifier.isFinal(testKClass.modifiers)) {
            """
                Test runner ${testKClass.simpleName} which inherits from ${runnerMarkerKClass.simpleName} and used as base class
                  for real test should have `open` modality 
            """.trimIndent()
        }
        val instance = testKClass.newInstance() as RunnerWithTargetBackendForTestGeneratorMarker
        return instance.targetBackend
    }
}
