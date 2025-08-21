/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.linkage.issues.SignatureClashDetector
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature

internal class JvmFieldSignatureClashDetector(
    private val classCodegen: ClassCodegen,
) : SignatureClashDetector<JvmMemberSignature.Field, IrField>() {

    override fun reportSignatureConflict(
        signature: JvmMemberSignature.Field,
        declarations: Collection<IrField>,
        diagnosticReporter: IrDiagnosticReporter,
    ) {
        reportSignatureClashTo(
            diagnosticReporter,
            JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS,
            declarations,
            JvmIrConflictingDeclarationsData(signature, declarations).render(),
            reportOnIfSynthetic = { classCodegen.irClass },
        )
    }
}
