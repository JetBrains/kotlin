package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.konan.library.MetadataReader
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.properties.propertyString
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute
import org.jetbrains.kotlin.serialization.konan.emptyPackages

class LibraryReaderImpl(
        override val libraryFile: File,
        val currentAbiVersion: Int,
        val target: KonanTarget? = null,
        override val isDefaultLibrary: Boolean = false,
        private val metadataReader: MetadataReader = DefaultMetadataReaderImpl
) : KonanLibraryReader {

    // For the zipped libraries inPlace gives files from zip file system
    // whereas realFiles extracts them to /tmp.
    // For unzipped libraries inPlace and realFiles are the same
    // providing files in the library directory.
    private val inPlace = KonanLibrary(libraryFile, target)
    private val realFiles = inPlace.realFiles

    override val manifestProperties: Properties by lazy {
        val properties = inPlace.manifestFile.loadProperties()
        if (target != null) substitute(properties, defaultTargetSubstitutions(target))
        properties
    }

    val abiVersion: String
        get() {
            val manifestAbiVersion = manifestProperties.getProperty("abi_version")
            check("$currentAbiVersion" == manifestAbiVersion) {
                "ABI version mismatch. Compiler expects: $currentAbiVersion, the library is $manifestAbiVersion"
            }
            return manifestAbiVersion
        }

    val targetList = inPlace.targetsDir.listFiles.map { it.name }
    override val dataFlowGraph by lazy { inPlace.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null } }

    override val libraryName
        get() = inPlace.libraryName

    override val uniqueName
        get() = manifestProperties.propertyString("unique_name")!!

    override val bitcodePaths: List<String>
        get() = (realFiles.kotlinDir.listFilesOrEmpty + realFiles.nativeDir.listFilesOrEmpty).map { it.absolutePath }

    override val includedPaths: List<String>
        get() = realFiles.includedDir.listFilesOrEmpty.map { it.absolutePath }

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList("linkerOpts", target!!.visibleName)

    override val unresolvedDependencies: List<String>
        get() = manifestProperties.propertyList("depends")

    override val resolvedDependencies = mutableListOf<KonanLibraryReader>()

    override val moduleHeaderData: ByteArray by lazy { metadataReader.loadSerializedModule(inPlace) }

    override var isNeededForLink: Boolean = false
        private set

    private val emptyPackages by lazy { emptyPackages(moduleHeaderData) }

    override fun markPackageAccessed(fqName: String) {
        if (!isNeededForLink // fast path
                && !emptyPackages.contains(fqName)) {
            isNeededForLink = true
        }
    }

    override fun packageMetadata(fqName: String) = metadataReader.loadSerializedPackageFragment(inPlace, fqName)
}
