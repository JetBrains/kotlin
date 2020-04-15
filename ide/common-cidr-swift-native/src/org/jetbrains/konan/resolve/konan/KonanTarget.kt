package org.jetbrains.konan.resolve.konan

interface KonanTarget {
    val moduleId: String
    val productModuleName: String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    companion object {
        const val PRODUCT_MODULE_NAME_KEY = "__CIDR_KonanProductModuleName"
    }
}