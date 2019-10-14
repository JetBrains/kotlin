package com.jetbrains.swift.codeinsight.resolve.module

import com.jetbrains.cidr.xcode.Xcode

class MobileSwiftApinotesCache : SwiftApinotesCacheImpl(Xcode.getBuildVersionString())