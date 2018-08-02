package org.jetbrains.konan.analyser.index

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializerProtocol
import org.jetbrains.kotlin.backend.konan.serialization.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

class KonanMetadataDecompiler : KonanMetadataDecompilerBase<KonanMetadataVersion>(
  KonanMetaFileType, KonanPlatform, KonanSerializerProtocol, NullFlexibleTypeDeserializer,
  KonanMetadataVersion.DEFAULT_INSTANCE, KonanMetadataVersion.INVALID_VERSION, KonanMetaFileType.STUB_VERSION
) {

  override fun doReadFile(file: VirtualFile): FileWithMetadata? {
    val proto = KonanDescriptorManager.getInstance().getCachedPackageFragment(file)
    return FileWithMetadata.Compatible(proto, KonanSerializerProtocol) //todo: check version compatibility
  }
}

class KonanMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
  override fun isCompatible(): Boolean = true //todo: ?

  companion object {
    @JvmField
    val DEFAULT_INSTANCE = KonanMetadataVersion(1, 1, 0)

    @JvmField
    val INVALID_VERSION = KonanMetadataVersion()
  }
}

object KonanMetaFileType : FileType {
  override fun getName() = "KNM"
  override fun getDescription() = "Kotlin/Native Metadata"
  override fun getDefaultExtension() = "knm"
  override fun getIcon() = null
  override fun isBinary() = true
  override fun isReadOnly() = true
  override fun getCharset(file: VirtualFile, content: ByteArray) = null

  const val STUB_VERSION = 2
}

class KonanMetaFileTypeFactory : FileTypeFactory() {

  override fun createFileTypes(consumer: FileTypeConsumer) = consumer.consume(KonanMetaFileType, KonanMetaFileType.defaultExtension)
}
