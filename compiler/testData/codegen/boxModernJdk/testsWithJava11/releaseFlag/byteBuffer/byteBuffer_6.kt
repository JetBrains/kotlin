// RELEASE: 6
// CHECK_BYTECODE_TEXT
// 1 public final static clear\(Ljava/nio/ByteBuffer;\)Ljava/nio/Buffer;
// 1 INVOKEVIRTUAL java/nio/ByteBuffer.clear \(\)Ljava/nio/Buffer;
fun clear(byteByffer: java.nio.ByteBuffer) = byteByffer.clear()

fun box(): String {
    if (clear(java.nio.ByteBuffer.allocateDirect(10)).capacity() != 10) return "fail"
    return "OK"
}