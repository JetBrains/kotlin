/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.settings

import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Path

data class KonanArtifact(
    val targetName: String,
    val moduleName: String,
    val type: CompilerOutputKind,
    val target: KonanTarget?,
    val libraryDependencies: List<String>,
    val sources: List<Path>,
    val output: Path
)

val CompilerOutputKind.isLibrary: Boolean get() = this == LIBRARY || this == DYNAMIC || this == STATIC || this == FRAMEWORK
val CompilerOutputKind.isExecutable: Boolean get() = this == PROGRAM
val CompilerOutputKind.isTest: Boolean get() = false //TODO
