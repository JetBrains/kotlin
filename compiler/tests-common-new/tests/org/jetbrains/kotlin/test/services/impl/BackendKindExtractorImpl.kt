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
        if (targetBackend == null) return BackendKind.NoBackend
        return if (targetBackend.isIR) BackendKinds.IrBackend
        else BackendKinds.ClassicBackend
    }
}
