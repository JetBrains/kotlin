package com.jetbrains.swift

import com.jetbrains.swift.codeinsight.resolve.module.SwiftModuleIOCache

class MobileSwiftInitializer {
    init {
        SwiftModuleIOCache.getInstance()
    }
}
