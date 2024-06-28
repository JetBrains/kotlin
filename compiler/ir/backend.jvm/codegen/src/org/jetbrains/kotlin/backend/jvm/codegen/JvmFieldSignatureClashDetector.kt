/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.linkage.SignatureClashDetector
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
import org.jetbrains.kotlin.resolve.jvm.diagnostics.RawSignature

internal class JvmFieldSignatureClashDetector(
    private val classCodegen: ClassCodegen,
) : SignatureClashDetector<RawSignature, IrField>() {

    override fun reportSignatureConflict(
        signature: RawSignature,
        declarations: Collection<IrField>,
        diagnosticReporter: IrDiagnosticReporter
    ) {
        reportSignatureClashTo(
            diagnosticReporter,
            JvmBackendErrors.CONFLICTING_JVM_DECLARATIONS,
            declarations,
            ConflictingJvmDeclarationsData(
                classInternalName = classCodegen.type.internalName,
                classOrigin = null,
                signature = signature,
                signatureOrigins = null,
                signatureDescriptors = declarations.map(IrDeclaration::toIrBasedDescriptor),
            ),
            reportOnIfSynthetic = { classCodegen.irClass },
        )
    }
}