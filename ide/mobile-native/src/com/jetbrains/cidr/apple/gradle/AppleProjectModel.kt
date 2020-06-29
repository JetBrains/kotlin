package com.jetbrains.cidr.apple.gradle

import java.io.File
import java.io.Serializable

interface AppleProjectModel : Serializable {
    val targets: Map<String, AppleTargetModel>
}

interface AppleTargetModel : Serializable {
    val name: String
    val sourceFolders: Set<File>
    val testFolders: Set<File>
    val editableXcodeProjectDir: File
    val bridgingHeader: File?
}

internal data class AppleProjectModelImpl(override val targets: Map<String, AppleTargetModel>) : AppleProjectModel
internal data class AppleTargetModelImpl(
    override val name: String,
    override val sourceFolders: Set<File>,
    override val testFolders: Set<File>,
    override val editableXcodeProjectDir: File,
    override val bridgingHeader: File?
) : AppleTargetModel
