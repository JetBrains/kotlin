package com.jetbrains.swift.sourcekitd

import com.jetbrains.cidr.xcode.XCLog
import com.jetbrains.swift.MobileSwiftCompilerSettings
import com.jetbrains.swift.SwiftCompilerSettings
import java.io.File

class MobileSourceKitServiceManager : SourceKitServiceManager() {
    override fun getToolchainDir(): File? {
        return try {
            val swiftCompilerSettings = SwiftCompilerSettings.getInstance() as MobileSwiftCompilerSettings
            File(swiftCompilerSettings.swiftToolchainPath).takeIf(File::isDirectory)
        } catch (e: SecurityException) {
            XCLog.LOG.warn(e)
            null
        }
    }
}
