/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl

fun FirSession.convertToIr(
    scopeSession: ScopeSession,
    firFiles: List<FirFile>,
    fir2IrExtensions: Fir2IrExtensions,
    irGeneratorExtensions: Collection<IrGenerationExtension>
): Fir2IrResult {
    val mangler = JvmDescriptorMangler(null)
    val signaturer = JvmIdSignatureDescriptor(mangler)

    val commonFirFiles = moduleData.dependsOnDependencies
        .map { it.session }
        .filter { it.kind == FirSession.Kind.Source }
        .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

    return Fir2IrConverter.createModuleFragment(
        this, scopeSession, firFiles + commonFirFiles,
        languageVersionSettings, signaturer,
        fir2IrExtensions,
        FirJvmKotlinMangler(this), JvmIrMangler, IrFactoryImpl,
        FirJvmVisibilityConverter,
        Fir2IrJvmSpecialAnnotationSymbolProvider(),
        irGeneratorExtensions
    )
}
