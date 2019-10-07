package com.jetbrains.cidr.apple.gradle

import java.io.File
import java.io.Serializable

interface AppleProjectModel : Serializable {
    val sourceSets: Map<String, AppleSourceSetModel>
}

interface AppleSourceSetModel : Serializable {
    val name: String
    val folderPaths: Set<File>
}

internal data class AppleProjectModelImpl(override val sourceSets: Map<String, AppleSourceSetModel>) : AppleProjectModel
internal data class AppleSourceSetModelImpl(override val name: String, override val folderPaths: Set<File>) : AppleSourceSetModel
