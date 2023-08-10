/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendInputHandler
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractIrHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput> = BackendKinds.IrBackend
) : BackendInputHandler<IrBackendInput>(testServices, artifactKind)
