package org.jetbrains.konan.resolve.konan

import com.jetbrains.cidr.xcode.model.PBXTarget

class AppCodeKonanBridgeTarget(val target: PBXTarget) : KonanBridgeTarget {
    override val name: String
        get() = target.name

    override fun equals(other: Any?): Boolean =
        other is AppCodeKonanBridgeTarget && target.id == other.target.id

    override fun hashCode(): Int = target.id.hashCode()
}