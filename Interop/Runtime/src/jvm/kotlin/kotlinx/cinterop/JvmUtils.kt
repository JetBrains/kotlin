package kotlinx.cinterop

internal fun decodeFromUtf8(bytes: ByteArray) = String(bytes)
internal fun encodeToUtf8(str: String) = str.toByteArray()