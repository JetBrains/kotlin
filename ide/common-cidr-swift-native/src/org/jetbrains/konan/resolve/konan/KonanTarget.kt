package org.jetbrains.konan.resolve.konan

interface KonanTarget {
    val moduleId: String
    val productModuleName: String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}