package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_VERSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KlibIrVersion(major: Int, minor: Int, patch: Int) : BinaryVersion(major, minor, patch) {

    override fun isCompatibleWithCurrentCompilerVersion(): Boolean = isCompatibleTo(INSTANCE)


    companion object {
        @JvmField
        val INSTANCE = KlibIrVersion(1, 0, 0)

        @JvmField
        val INVALID_VERSION = KlibIrVersion(-1, -1, -1)
    }
}

fun KlibIrVersion(vararg values: Int): KlibIrVersion {
    if (values.size != 3) error("Ir version should be in major.minor.patch format: $values")
    return KlibIrVersion(values[0], values[1], values[2])
}

val KotlinLibrary.metadataVersion: KlibIrVersion
    get() {
        val versionString = manifestProperties.getProperty(KLIB_PROPERTY_IR_VERSION)
        val versionIntArray = BinaryVersion.parseVersionArray(versionString)
            ?: error("Could not parse ir version: $versionString")
        return KlibIrVersion(*versionIntArray)
    }