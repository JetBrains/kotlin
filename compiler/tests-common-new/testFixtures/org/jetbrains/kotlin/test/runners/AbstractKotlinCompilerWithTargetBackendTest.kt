/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TargetBackend.*
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.testFederation.AffectedByCompiler
import org.jetbrains.kotlin.testFederation.AffectedByJs
import org.jetbrains.kotlin.testFederation.AffectedByNative
import org.jetbrains.kotlin.testFederation.AffectedByWasm

@RequiresOptIn("Consider using predefined inheritors of this class with properly configured test domains. " +
            "If they don't suite please specify domains yourself")
annotation class UnspecifiedTargetBackend

abstract class AbstractKotlinCompilerWithTargetBackendTest @UnspecifiedTargetBackend constructor(
    val targetBackend: TargetBackend,
) : AbstractKotlinCompilerTest() {
    @TestInfrastructureInternals
    final override fun configureInternal(builder: TestConfigurationBuilder) {
        val myTargetBackend = targetBackend
        configure(builder)
        with(builder) {
            globalDefaults {
                if (targetBackend == null) {
                    targetBackend = myTargetBackend
                } else {
                    require(targetBackend == myTargetBackend) {
                        """Target backend in configuration specified to $targetBackend but in 
                          |AbstractKotlinCompilerWithTargetBackendTest parent it is set to $myTargetBackend""".trimMargin()
                    }
                }
            }
        }
    }
}


@OptIn(UnspecifiedTargetBackend::class)
@AffectedByCompiler
abstract class AbstractKotlinCompilerJvmTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR)

@OptIn(UnspecifiedTargetBackend::class)
@AffectedByJs
abstract class AbstractKotlinCompilerJsTest(targetBackend: TargetBackend) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    init {
        require(targetBackend == JS_IR || targetBackend == JS_IR_ES6)
    }
}

@OptIn(UnspecifiedTargetBackend::class)
@AffectedByWasm
abstract class AbstractKotlinCompilerWasmTest(targetBackend: TargetBackend) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    init {
        require(targetBackend == WASM || targetBackend == WASM_JS || targetBackend == WASM_WASI)
    }
}

@OptIn(UnspecifiedTargetBackend::class)
@AffectedByNative
abstract class AbstractKotlinCompilerNativeTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE)

@OptIn(UnspecifiedTargetBackend::class)
@AffectedByCompiler
abstract class AbstractKotlinCompilerJKlibTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JKLIB)
