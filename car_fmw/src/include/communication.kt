
external fun VCP_init()
external fun clear_buffer()

external fun send_int(i: Int)
external fun send_buffer(size: Int, pointer: Int)

external fun receive_int(): Int
external fun receive_buffer(size: Int, pointer: Int)

fun receiveByteArray(): ByteArray {
    val result = ByteArray(receive_int())
    receive_buffer(result.size, result.data)

    return result
}

fun sendByteArray(arr: ByteArray) {
    send_buffer(arr.size, arr.data)
}