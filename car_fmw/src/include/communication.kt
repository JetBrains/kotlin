
external fun VCP_init()
external fun send_int(i: Int)
external fun send_buffer(size: Int, pointer: Int)

fun sendByteArray(arr: ByteArray) {
    send_buffer(arr.size, arr.data)
}