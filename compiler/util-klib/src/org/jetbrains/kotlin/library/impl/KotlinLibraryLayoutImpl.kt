package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.nio.file.FileSystem

open class KotlinLibraryLayoutImpl(val klib: File, override val component: String?) : KotlinLibraryLayout {
    val isZipped = klib.isFile

    init {
        if (isZipped) zippedKotlinLibraryChecks(klib)
    }

    override val libFile = if (isZipped) File("/") else klib

    override val libraryName
        get() =
            if (isZipped)
                klib.path.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT)
            else
                libFile.path

    open fun directlyFromZip(zipFileSystem: FileSystem): KotlinLibraryLayout =
        FromZipBaseLibraryImpl(this, zipFileSystem)

}

class MetadataLibraryLayoutImpl(klib: File, component: String) : KotlinLibraryLayoutImpl(klib, component), MetadataKotlinLibraryLayout {

    override fun directlyFromZip(zipFileSystem: FileSystem): MetadataKotlinLibraryLayout =
        FromZipMetadataLibraryImpl(this, zipFileSystem)
}

class IrLibraryLayoutImpl(klib: File, component: String) : KotlinLibraryLayoutImpl(klib, component), IrKotlinLibraryLayout {

    override fun directlyFromZip(zipFileSystem: FileSystem): IrKotlinLibraryLayout =
        FromZipIrLibraryImpl(this, zipFileSystem)
}

@Suppress("UNCHECKED_CAST")
open class BaseLibraryAccess<L : KotlinLibraryLayout>(val klib: File, component: String?, zipAccessor: ZipFileSystemAccessor? = null) {
    open val layout = KotlinLibraryLayoutImpl(klib, component)

    private val klibZipAccessor = zipAccessor ?: ZipFileSystemInPlaceAccessor

    fun <T> inPlace(action: (L) -> T): T =
        if (layout.isZipped)
            klibZipAccessor.withZipFileSystem(layout.klib) { zipFileSystem ->
                action(layout.directlyFromZip(zipFileSystem) as L)
            }
        else
            action(layout as L)
}


class MetadataLibraryAccess<L : KotlinLibraryLayout>(klib: File, component: String, zipAccessor: ZipFileSystemAccessor? = null) :
    BaseLibraryAccess<L>(klib, component, zipAccessor) {
    override val layout = MetadataLibraryLayoutImpl(klib, component)
}

class IrLibraryAccess<L : KotlinLibraryLayout>(klib: File, component: String, zipAccessor: ZipFileSystemAccessor? = null) :
    BaseLibraryAccess<L>(klib, component, zipAccessor) {
    override val layout = IrLibraryLayoutImpl(klib, component)
}

open class FromZipBaseLibraryImpl(zipped: KotlinLibraryLayoutImpl, zipFileSystem: FileSystem) :
    KotlinLibraryLayout {

    override val libraryName = zipped.libraryName
    override val libFile = zipFileSystem.file(zipped.libFile)
    override val component = zipped.component
}

class FromZipMetadataLibraryImpl(zipped: MetadataLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), MetadataKotlinLibraryLayout

class FromZipIrLibraryImpl(zipped: IrLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), IrKotlinLibraryLayout

internal fun zippedKotlinLibraryChecks(klibFile: File) {
    check(klibFile.exists) { "Could not find $klibFile." }
    check(klibFile.isFile) { "Expected $klibFile to be a regular file." }

    val extension = klibFile.extension
    check(extension.isEmpty() || extension == KLIB_FILE_EXTENSION || extension == "jar") {
        "KLIB path has unexpected extension: $klibFile"
    }
}
