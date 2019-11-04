package org.jetbrains.konan.resolve.konan

interface KonanTarget {
    val name: String
    val productModuleName: String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}