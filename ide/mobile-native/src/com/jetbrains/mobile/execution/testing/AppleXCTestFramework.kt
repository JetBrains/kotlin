package com.jetbrains.mobile.execution.testing

import com.intellij.psi.PsiFile
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import com.jetbrains.cidr.execution.CidrBuildTarget
import com.jetbrains.cidr.lang.OCTestFramework
import com.jetbrains.mobile.execution.MobileBuildConfigurationHelper
import com.jetbrains.swift.execution.testing.SwiftUnitTestFrameworkBase

class AppleXCTestFramework : SwiftUnitTestFrameworkBase() {
    override fun getTargetsForFile(file: PsiFile): Set<CidrBuildTarget<out CidrBuildConfiguration>> =
        MobileBuildConfigurationHelper(file.project).targets.toSet()

    companion object {
        val instance: AppleXCTestFramework
            get() = OCTestFramework.getInstance(AppleXCTestFramework::class.java)
    }
}