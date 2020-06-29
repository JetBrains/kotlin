/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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