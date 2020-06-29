package org.jetbrains.konan.resolve.konan

import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.rawBuildSettings

class AppCodeKonanTarget(val target: PBXTarget) : KonanTarget {
    override val moduleId: String
        get() = ":" + target.name

    override val productModuleName: String
        get() = target.preferredConfiguration?.rawBuildSettings?.productModuleName ?: target.name

    override fun equals(other: Any?): Boolean =
        other is AppCodeKonanTarget && target.id == other.target.id

    override fun hashCode(): Int = target.id.hashCode()
}