/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.services.BackendKindExtractor
import org.jetbrains.kotlin.test.services.TestServices

class BackendKindExtractorImpl(testServices: TestServices) : BackendKindExtractor(testServices) {
    override fun backendKind(targetBackend: TargetBackend?): BackendKind<*> {
        return when (targetBackend) {
            TargetBackend.ANY,
            TargetBackend.JVM,
            TargetBackend.JVM_OLD,
            TargetBackend.ANDROID,
            TargetBackend.JVM_MULTI_MODULE_OLD_AGAINST_IR -> BackendKinds.ClassicBackend

            TargetBackend.JVM_IR,
            TargetBackend.JVM_MULTI_MODULE_IR_AGAINST_OLD,
            TargetBackend.JS,
            TargetBackend.JS_IR,
            TargetBackend.JS_IR_ES6,
            TargetBackend.WASM -> BackendKinds.IrBackend

            null -> BackendKind.NoBackend
        }
    }
}
