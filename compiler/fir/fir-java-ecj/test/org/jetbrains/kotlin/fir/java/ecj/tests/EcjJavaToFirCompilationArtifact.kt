/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.ResultingArtifact
import java.io.File

/**
 * Artifact that holds the result of converting a Java source file to FIR using ECJ.
 *
 * @property sourceFile The Java source file that was converted
 * @property javaSource The Java source code as a string
 * @property firJavaClass The resulting FIR Java class from the conversion
 * @property diagnostics Any diagnostics that were generated during the conversion
 */
data class EcjJavaToFirCompilationArtifact(
    val sourceFile: File,
    val javaSource: String,
    val firJavaClass: FirJavaClass?,
    val diagnostics: List<String> = emptyList(),
    val session: FirSession? = null,
) : ResultingArtifact.Binary<EcjJavaToFirCompilationArtifact>() {
    object Kind : ArtifactKind<EcjJavaToFirCompilationArtifact>("EcjJavaToFirCompilationArtifact")

    override val kind: ArtifactKind<EcjJavaToFirCompilationArtifact> get() = Kind
}