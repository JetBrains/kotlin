/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import org.jetbrains.kotlin.gradle.KonanArtifactModel
import org.jetbrains.kotlin.gradle.KonanArtifactModelImpl

data class KonanModelImpl(
    override val artifacts: List<KonanArtifactModel>,
    override val buildTaskPath: String,
    override val cleanTaskPath: String,
    override val kotlinNativeHome: String
) : KonanModel {
    constructor(konanModel: KonanModel) : this(
        konanModel.artifacts.map { KonanArtifactModelImpl(it) },
        konanModel.buildTaskPath,
        konanModel.cleanTaskPath,
        konanModel.kotlinNativeHome
    )
}

