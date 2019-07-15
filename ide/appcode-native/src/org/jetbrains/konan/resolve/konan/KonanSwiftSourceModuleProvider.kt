package org.jetbrains.konan.resolve.konan

import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.model.XcodeMetaData.getTargetFor
import com.jetbrains.swift.codeinsight.resolve.SwiftModule
import com.jetbrains.swift.codeinsight.resolve.SwiftSourceModuleProvider

class KonanSwiftSourceModuleProvider : SwiftSourceModuleProvider {
    override fun createModule(configuration: OCResolveConfiguration): SwiftModule =
        KonanSwiftSourceModule(configuration, getTargetFor(configuration)!!, configuration)

    override fun isAvailable(configuration: OCResolveConfiguration): Boolean {
        val target = getTargetFor(configuration)
        return target != null &&
               target.isFramework &&
               sequenceOf(target).containsKotlinNativeTargets(configuration.project)
    }
}