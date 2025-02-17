/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact

class IrBackendInputsFromK1AndK2(
    val fromK1: IrBackendInput,
    val fromK2: IrBackendInput,
) : ResultingArtifact.BackendInput<IrBackendInputsFromK1AndK2>() {
    override val kind: BackendKind<IrBackendInputsFromK1AndK2>
        get() = BackendKinds.IrBackendForK1AndK2


}