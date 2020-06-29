package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.swift.codeinsight.resolve.AppcodeSourceModuleProducer
import com.jetbrains.swift.codeinsight.resolve.SwiftModule

class KonanSwiftSourceModuleProvider : AppcodeSourceModuleProducer {

    override fun create(
        configuration: OCResolveConfiguration,
        target: PBXTarget
    ): SwiftModule? {
        if (target.isKotlinTarget(configuration.project)) {
            return AppCodeKonanSourceModule(configuration, target)
        }

        return null
    }

    private fun PBXTarget.isKotlinTarget(project: Project): Boolean =
        sequenceOf(this).containsKotlinNativeTargets(project)
}