/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_DIFFERENCE
import org.jetbrains.kotlin.test.directives.TestDumpClassifier
import org.jetbrains.kotlin.test.directives.TestDumpRoot
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure

enum class TargetBackendClassifier(override val compatibleWith: TargetBackendClassifier? = null) :
    TestDumpClassifier<TargetBackendClassifier.Root> {
    JVM,
    JVM_IR(JVM),
    JVM_IR_SERIALIZE(JVM_IR),
    JS_IR,
    JS_IR_ES6(JS_IR),
    WASM,
    WASM_JS(WASM),
    WASM_WASI(WASM),
    ANDROID(JVM),
    NATIVE,
    JKLIB(JVM_IR)
    ;

    override val extension: String get() = name.lowercase()
    override val root get() = Root

    companion object Root : TestDumpRoot<Root>("targetBackend") {
        override fun calculateClassifiers() =
            TargetBackendClassifier.entries

        override fun fixedClassifiers(directives: RegisteredDirectives): List<TestDumpClassifier<Root>> {
            return directives[DUMP_IR_DIFFERENCE].map { TargetBackendClassifier.valueOf(it.name) }
        }
    }
}

internal fun TestServices.getMatchedBackendFromDirective(directive: ValueDirective<TargetBackendClassifier>): TargetBackend? {
    val backendsInDirective = moduleStructure.allDirectives[directive].map { TargetBackend.valueOf(it.name) }.toSet()
    var current = defaultsProvider.targetBackend ?: return null
    while (current != TargetBackend.ANY) {
        if (current in backendsInDirective) return current
        current = current.compatibleWith
    }
    return null
}
