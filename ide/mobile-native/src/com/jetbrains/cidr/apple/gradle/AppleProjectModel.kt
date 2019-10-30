package com.jetbrains.cidr.apple.gradle

import com.jetbrains.swift.symbols.SwiftBridgeTarget
import org.jetbrains.konan.resolve.konan.KonanBridgeTarget
import java.io.File
import java.io.Serializable

interface AppleProjectModel : Serializable {
    val targets: Map<String, AppleTargetModel>
}

interface AppleTargetModel : Serializable, KonanBridgeTarget, SwiftBridgeTarget {
    override val name: String
    val sourceFolders: Set<File>
    val bridgingHeader: File?
}

internal data class AppleProjectModelImpl(override val targets: Map<String, AppleTargetModel>) : AppleProjectModel
internal data class AppleTargetModelImpl(
    override val name: String,
    override val sourceFolders: Set<File>,
    override val bridgingHeader: File?
) : AppleTargetModel
