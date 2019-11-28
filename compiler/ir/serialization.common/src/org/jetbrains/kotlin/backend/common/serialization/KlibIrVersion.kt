package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_VERSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KlibIrVersion(vararg numbers: Int) : BinaryVersion(*numbers) {

    override fun isCompatible(): Boolean = isCompatibleTo(INSTANCE)


    companion object {
        @JvmField
        val INSTANCE = KlibIrVersion(1, 0, 0)

        @JvmField
        val INVALID_VERSION = KlibIrVersion(-1, -1, -1)
    }
}

val KotlinLibrary.metadataVersion: KlibIrVersion
    get() {
        val versionString = manifestProperties.getProperty(KLIB_PROPERTY_IR_VERSION)
        val versionIntArray = BinaryVersion.parseVersionArray(versionString)
            ?: error("Could not parse metadata version: $versionString")
        return KlibIrVersion(*versionIntArray)
    }