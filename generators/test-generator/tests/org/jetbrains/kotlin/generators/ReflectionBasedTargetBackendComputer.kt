/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.model.DefaultTargetBackendComputer
import org.jetbrains.kotlin.generators.model.TargetBackendComputer
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.runners.RunnerWithTargetBackendForTestGeneratorMarker
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf

object ReflectionBasedTargetBackendComputer : TargetBackendComputer {
    private val runnerMarkerKClass = RunnerWithTargetBackendForTestGeneratorMarker::class
    private const val TARGET_BACKEND_PROPERTY_NAME = "targetBackend"

    override fun compute(definedTargetBackend: TargetBackend?, testKClass: KClass<*>): TargetBackend {
        if (!testKClass.isSubclassOf(runnerMarkerKClass)) return DefaultTargetBackendComputer.compute(definedTargetBackend, testKClass)
        require(definedTargetBackend == null) {
            """
                Test ${testKClass.simpleName} is inheritor of ${runnerMarkerKClass.simpleName} which means that
                   target you should not specify targetBackend in test generation DSL, because it will be 
                   read from test runner class itself
            """.trimIndent()
        }
        require(testKClass.isOpen) {
            """
                Test runner which inherits from ${runnerMarkerKClass.simpleName} and used as base class
                  for real test should have `open` modality 
            """.trimIndent()
        }
        val instance = testKClass.createInstance() as RunnerWithTargetBackendForTestGeneratorMarker
        val kProperty = runnerMarkerKClass.declaredMemberProperties.single { it.name == TARGET_BACKEND_PROPERTY_NAME }
        return kProperty.get(instance) as TargetBackend
    }
}
