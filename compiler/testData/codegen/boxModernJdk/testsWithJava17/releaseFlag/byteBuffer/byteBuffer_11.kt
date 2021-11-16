// JDK_RELEASE: 11
// CHECK_BYTECODE_TEXT
// 1 public final static clear\(Ljava/nio/ByteBuffer;\)Ljava/nio/ByteBuffer;
// 1 INVOKEVIRTUAL java/nio/ByteBuffer.clear \(\)Ljava/nio/ByteBuffer;
fun clear(byteByffer: java.nio.ByteBuffer) = byteByffer.clear()

fun box(): String {
    if (clear(java.nio.ByteBuffer.allocateDirect(10)).capacity() != 10) return "fail"
    return "OK"
}