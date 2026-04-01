/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibSerializationArtifact
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.CompilationStage
import java.io.File

object JKlibArtifactKinds {
    object JKlibKLib : ArtifactKind<JKlibKLibWithArtifact>("JKlibKLib", CompilationStage.FIRST)
}

class JKlibKLibWithArtifact(
    val outputFile: File,
    val reporter: BaseDiagnosticsCollector,
    val serializationArtifact: JKlibSerializationArtifact
) : ResultingArtifact.Binary<JKlibKLibWithArtifact>() {
    override val kind: ArtifactKind<JKlibKLibWithArtifact> get() = JKlibArtifactKinds.JKlibKLib
}
