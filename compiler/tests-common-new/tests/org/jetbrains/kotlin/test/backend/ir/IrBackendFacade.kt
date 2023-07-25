/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices

abstract class IrBackendFacade<BinaryOutputArtifact : ResultingArtifact.Binary<BinaryOutputArtifact>>(
    testServices: TestServices,
    binaryKind: BinaryKind<BinaryOutputArtifact>
) : BackendFacade<IrBackendInput, BinaryOutputArtifact>(testServices, BackendKinds.IrBackend, binaryKind)
