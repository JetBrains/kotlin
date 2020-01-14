package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.withZipFileSystem
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.nio.file.FileSystem

open class KotlinLibraryLayoutImpl(val klib: File, override val component: String?) : KotlinLibraryLayout {
    val isZipped = klib.isFile

    init {
        if (isZipped) zippedKotlinLibraryChecks(klib)
    }

    override val libDir = if (isZipped) File("/") else klib

    override val libraryName
        get() =
            if (isZipped)
                klib.path.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT)
            else
                libDir.path

    open val extractingToTemp: KotlinLibraryLayout by lazy {
        ExtractingBaseLibraryImpl(this)
    }

    open fun directlyFromZip(zipFileSystem: FileSystem): KotlinLibraryLayout =
        FromZipBaseLibraryImpl(this, zipFileSystem)

}

class MetadataLibraryLayoutImpl(klib: File, component: String) : KotlinLibraryLayoutImpl(klib, component), MetadataKotlinLibraryLayout {

    override val extractingToTemp: MetadataKotlinLibraryLayout by lazy {
        ExtractingMetadataLibraryImpl(this)
    }

    override fun directlyFromZip(zipFileSystem: FileSystem): MetadataKotlinLibraryLayout =
        FromZipMetadataLibraryImpl(this, zipFileSystem)
}

class IrLibraryLayoutImpl(klib: File, component: String) : KotlinLibraryLayoutImpl(klib, component), IrKotlinLibraryLayout {

    override val extractingToTemp: IrKotlinLibraryLayout by lazy {
        ExtractingIrLibraryImpl(this)
    }

    override fun directlyFromZip(zipFileSystem: FileSystem): IrKotlinLibraryLayout =
        FromZipIrLibraryImpl(this, zipFileSystem)
}

open class BaseLibraryAccess<L : KotlinLibraryLayout>(val klib: File, component: String?) {
    open val layout = KotlinLibraryLayoutImpl(klib, component)

    fun <T> realFiles(action: (L) -> T): T =
        if (layout.isZipped)
            action(layout.extractingToTemp as L)
        else
            action(layout as L)

    fun <T> inPlace(action: (L) -> T): T =
        if (layout.isZipped)
            layout.klib.withZipFileSystem { zipFileSystem ->
                action(layout.directlyFromZip(zipFileSystem) as L)
            }
        else
            action(layout as L)
}


open class MetadataLibraryAccess<L : KotlinLibraryLayout>(klib: File, component: String) : BaseLibraryAccess<L>(klib, component) {
    override val layout = MetadataLibraryLayoutImpl(klib, component)
}

open class IrLibraryAccess<L : KotlinLibraryLayout>(klib: File, component: String) : BaseLibraryAccess<L>(klib, component) {
    override val layout = IrLibraryLayoutImpl(klib, component)
}

open class FromZipBaseLibraryImpl(zipped: KotlinLibraryLayoutImpl, zipFileSystem: FileSystem) :
    KotlinLibraryLayout {

    override val libraryName = zipped.libraryName
    override val libDir = zipFileSystem.file(zipped.libDir)
    override val component = zipped.component
}

class FromZipMetadataLibraryImpl(zipped: MetadataLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), MetadataKotlinLibraryLayout

class FromZipIrLibraryImpl(zipped: IrLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), IrKotlinLibraryLayout

/**
 * This class and its children automatically extracts pieces of the library on first access. Use it if you need
 * to pass extracted files to an external tool. Otherwise, stick to [FromZipBaseLibraryImpl].
 */
fun KotlinLibraryLayoutImpl.extract(file: File): File = this.klib.withZipFileSystem { zipFileSystem ->
    val temporary = org.jetbrains.kotlin.konan.file.createTempFile(file.name)
    zipFileSystem.file(file).copyTo(temporary)
    temporary.deleteOnExit()
    temporary
}

fun KotlinLibraryLayoutImpl.extractDir(directory: File): File = this.klib.withZipFileSystem { zipFileSystem ->
    val temporary = org.jetbrains.kotlin.konan.file.createTempDir(directory.name)
    zipFileSystem.file(directory).recursiveCopyTo(temporary)
    temporary.deleteOnExitRecursively()
    temporary
}

open class ExtractingKotlinLibraryLayout(zipped: KotlinLibraryLayoutImpl) : KotlinLibraryLayout {
    override val libDir: File get() = error("Extracting layout doesn't extract its own root")
    override val libraryName = zipped.libraryName
    override val component = zipped.component
}

open class ExtractingBaseLibraryImpl(zipped: KotlinLibraryLayoutImpl) :
    ExtractingKotlinLibraryLayout(zipped) {
    override val manifestFile: File by lazy { zipped.extract(zipped.manifestFile) }
    override val resourcesDir: File by lazy { zipped.extractDir(zipped.resourcesDir) }
}

class ExtractingMetadataLibraryImpl(val zipped: MetadataLibraryLayoutImpl) :
    ExtractingKotlinLibraryLayout(zipped),
    MetadataKotlinLibraryLayout {

    override val metadataDir by lazy { zipped.extractDir(zipped.metadataDir) }
}

class ExtractingIrLibraryImpl(val zipped: IrLibraryLayoutImpl) :
    ExtractingKotlinLibraryLayout(zipped),
    IrKotlinLibraryLayout {

    override val irDeclarations: File by lazy { zipped.extract(zipped.irDeclarations) }

    override val irSymbols: File by lazy { zipped.extract(zipped.irSymbols) }

    override val irTypes: File by lazy { zipped.extract(zipped.irTypes) }

    override val irStrings: File by lazy { zipped.extract(zipped.irStrings) }

    override val irBodies: File by lazy { zipped.extract(zipped.irBodies) }

    override val irFiles: File by lazy { zipped.extract(zipped.irFiles) }
}

internal fun zippedKotlinLibraryChecks(klibFile: File) {
    check(klibFile.exists) { "Could not find $klibFile." }
    check(klibFile.isFile) { "Expected $klibFile to be a regular file." }

    val extension = klibFile.extension
    check(extension.isEmpty() || extension == KLIB_FILE_EXTENSION || extension == "jar") {
        "KLIB path has unexpected extension: $klibFile"
    }
}