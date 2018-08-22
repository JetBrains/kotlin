package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.MetadataReader

object DefaultMetadataReaderImpl : MetadataReader {

    override fun loadSerializedModule(library: KonanLibrary): ByteArray = library.moduleHeaderFile.readBytes()

    override fun loadSerializedPackageFragment(library: KonanLibrary, fqName: String): ByteArray =
            library.packageFile(fqName).readBytes()
}
