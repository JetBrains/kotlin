package com.jetbrains.mobile.execution.testing

import com.intellij.psi.PsiFile
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import com.jetbrains.cidr.execution.CidrBuildTarget
import com.jetbrains.cidr.lang.OCTestFramework
import com.jetbrains.swift.execution.testing.SwiftUnitTestFrameworkBase

class AppleXCTestFramework : SwiftUnitTestFrameworkBase() {
    override fun getTargetsForFile(file: PsiFile): Set<CidrBuildTarget<out CidrBuildConfiguration>> = emptySet()

    companion object {
        val instance: AppleXCTestFramework
            get() = OCTestFramework.getInstance(AppleXCTestFramework::class.java)
    }
}