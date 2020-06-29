/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper

class MobileBuildConfigurationHelper(val project: Project) : CidrBuildConfigurationHelper<MobileBuildConfiguration, MobileBuildTarget>() {
    override fun allowEditBuildConfiguration(): Boolean = false

    override fun getTargets(): List<MobileBuildTarget> = emptyList()

    override fun findSimilarValidInTargets(
        buildTarget: MobileBuildTarget?,
        buildConfiguration: MobileBuildConfiguration?,
        buildTargets: List<MobileBuildTarget>
    ): BuildTargetAndConfigurationData? = null
}