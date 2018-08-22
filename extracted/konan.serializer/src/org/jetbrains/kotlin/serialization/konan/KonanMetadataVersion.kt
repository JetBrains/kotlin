package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KonanMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {

    override fun isCompatible(): Boolean = this.major == 1 && this.minor == 0

    companion object {
        @JvmField
        val INSTANCE = KonanMetadataVersion(1, 0, 0)

        @JvmField
        val INVALID_VERSION = KonanMetadataVersion()
    }
}
