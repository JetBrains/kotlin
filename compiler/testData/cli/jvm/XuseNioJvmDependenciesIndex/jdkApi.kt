fun clear(byteBuffer: java.nio.ByteBuffer) = byteBuffer.clear()

fun path(): java.nio.file.Path? = null

fun stream() = java.util.stream.Stream.of(42)

// `Base64.sig` and `Base64$Encoder.sig` are in different release folders of ct.sym
fun encode(bytes: ByteArray): ByteArray = java.util.Base64.getEncoder().encode(bytes)
