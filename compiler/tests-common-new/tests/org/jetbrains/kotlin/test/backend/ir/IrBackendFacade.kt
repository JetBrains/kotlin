/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.DeserializerFacade
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.TestServices

abstract class IrBackendFacade<BinaryOutputArtifact : ResultingArtifact.Binary<BinaryOutputArtifact>>(
    testServices: TestServices,
    artifactKind: ArtifactKind<BinaryOutputArtifact>
) : BackendFacade<IrBackendInput, BinaryOutputArtifact>(testServices, BackendKinds.IrBackend, artifactKind)

data class KlibFacades(
    val serializerFacade: Constructor<IrBackendFacade<BinaryArtifacts.KLib>>,
    val deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>,
)
