/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class TypeApproximatorForMetadataSerializer(session: FirSession) :
    AbstractTypeApproximator(session.typeContext, session.languageVersionSettings) {

    override fun createErrorType(debugName: String): SimpleTypeMarker {
        return ConeErrorType(ConeIntermediateDiagnostic(debugName))
    }
}

