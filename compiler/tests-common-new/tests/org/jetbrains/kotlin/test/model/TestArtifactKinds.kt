/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact

object FrontendKinds {
    object ClassicFrontend : FrontendKind<ClassicFrontendOutputArtifact>("ClassicFrontend")
    object FIR : FrontendKind<FirOutputArtifact>("FIR")

    fun fromString(string: String): FrontendKind<*>? {
        return when (string) {
            "ClassicFrontend" -> ClassicFrontend
            "FIR" -> FIR
            else -> null
        }
    }
}

val FrontendKind<*>.isFir: Boolean
    get() = this == FrontendKinds.FIR

object BackendKinds {
    object ClassicBackend : BackendKind<ClassicBackendInput>("ClassicBackend")
    object IrBackend : BackendKind<IrBackendInput>("IrBackend")

    fun fromString(string: String): BackendKind<*>? {
        return when (string) {
            "ClassicBackend" -> ClassicBackend
            "IrBackend" -> IrBackend
            else -> null
        }
    }

    fun fromTargetBackend(targetBackend: TargetBackend?): BackendKind<*> {
        if (targetBackend == null) return BackendKind.NoBackend
        return if (targetBackend.isIR) IrBackend
        else ClassicBackend
    }
}

object ArtifactKinds {
    object Jvm : BinaryKind<BinaryArtifacts.Jvm>("JVM")
    object Js : BinaryKind<BinaryArtifacts.Js>("JS")
    object Native : BinaryKind<BinaryArtifacts.Native>("Native")
    object KLib : BinaryKind<BinaryArtifacts.KLib>("KLib")

    fun fromString(string: String): BinaryKind<*>? {
        return when (string) {
            "Jvm" -> Jvm
            "Js" -> Js
            "Native" -> Native
            "KLib" -> KLib
            else -> null
        }
    }
}
