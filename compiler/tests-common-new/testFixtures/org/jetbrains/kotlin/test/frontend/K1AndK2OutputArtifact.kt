/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend

import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact

class K1AndK2OutputArtifact(val k1Artifact: ClassicFrontendOutputArtifact, val k2Artifact: FirOutputArtifact) :
    ResultingArtifact.FrontendOutput<K1AndK2OutputArtifact>() {
    override val kind: FrontendKind<K1AndK2OutputArtifact>
        get() = FrontendKinds.ClassicAndFIR
}