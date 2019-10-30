package org.jetbrains.konan.resolve.konan

interface KonanBridgeTarget {
    val name: String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}