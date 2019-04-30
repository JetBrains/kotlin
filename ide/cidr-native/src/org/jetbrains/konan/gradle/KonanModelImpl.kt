/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File

data class KonanModelImpl(
    override val artifacts: List<KonanModelArtifact>,
    override val buildTaskPath: String,
    override val cleanTaskPath: String,
    override val kotlinNativeHome: String
) : KonanModel

data class KonanModelArtifactImpl(
    override val name: String,
    override val type: CompilerOutputKind,
    override val targetPlatform: String,
    override val file: File,
    override val buildTaskPath: String,
    override val isTests: Boolean
) : KonanModelArtifact
