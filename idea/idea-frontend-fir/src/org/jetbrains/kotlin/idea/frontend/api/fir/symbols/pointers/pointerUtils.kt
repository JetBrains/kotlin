/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrSignatureComposer
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmMangleComputer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.idea.fir.low.level.api.ideSessionComponents
import org.jetbrains.kotlin.ir.util.IdSignature

internal inline fun <reified D : FirDeclaration> Collection<FirDeclaration>.findDeclarationWithSignature(
    signature: IdSignature,
    firSession: FirSession
): D? {
    val signatureComposer = firSession.ideSessionComponents.signatureComposer
    for (declaration in this) {
        if (declaration is D && signatureComposer.composeSignature(declaration) == signature) {
            return declaration
        }
    }
    return null
}

internal fun FirDeclaration.createSignature(): IdSignature {
    val signatureComposer = session.ideSessionComponents.signatureComposer
    return signatureComposer.composeSignature(this)
        ?: error("Could not compose signature for declaration, looks like it is private or local")
}
