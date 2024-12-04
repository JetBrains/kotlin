/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.linkage.SignatureClashDetector
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentClassOrNull

class IdSignatureClashDetector : SignatureClashDetector<IdSignature, IrDeclaration>() {
    override fun reportSignatureConflict(
        signature: IdSignature,
        declarations: Collection<IrDeclaration>,
        diagnosticReporter: IrDiagnosticReporter
    ) {
        reportSignatureClashTo(
            diagnosticReporter,
            SerializationErrors.CONFLICTING_KLIB_SIGNATURES_ERROR,
            declarations,
            ConflictingKlibSignaturesData(signature, declarations),
            reportOnIfSynthetic = { it.parentClassOrNull },
        )
    }
}