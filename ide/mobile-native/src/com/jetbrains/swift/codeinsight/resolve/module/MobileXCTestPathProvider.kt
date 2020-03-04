/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.swift.codeinsight.resolve.module

import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.Xcode

class MobileXCTestPathProvider : SwiftCustomIncludePathProvider {
    override fun getCustomLibrarySearchPaths(configuration: OCResolveConfiguration): List<String> =
        if (Xcode.isInstalled())
            listOf(
                Xcode.getSubFilePath("Platforms/iPhoneSimulator.platform/Developer/Library/Frameworks")
            )
        else emptyList()
}