
external fun car_conn_init()
external fun car_conn_rcv_buf_clear()

external fun car_conn_snd_byte(b: Byte)
external fun car_conn_snd_int(i: Int)
external fun car_conn_snd_buf(size: Int, pointer: Int)

external fun car_conn_rcv_byte(): Byte
external fun car_conn_rcv_int(): Int
external fun car_conn_rcv_buf(size: Int, pointer: Int)

object Connection {
    fun init() {
        car_conn_init()
    }

    fun clearBuffer() {
        car_conn_rcv_buf_clear()
    }

    fun receiveInt(): Int = car_conn_rcv_int()
    fun receiveByte(): Byte = car_conn_rcv_byte()

    fun sendInt(n: Int) {
        car_conn_snd_int(n)
    }

    fun sendByte(b: Byte) {
        car_conn_snd_byte(b)
    }

    fun receiveByteArray(): ByteArray {
        val result = ByteArray(car_conn_rcv_int())
        car_conn_rcv_buf(result.size, result.data)

        return result
    }

    fun sendByteArray(array: ByteArray) {
        car_conn_snd_int(array.size)
        car_conn_rcv_buf(array.size, array.data)
    }
}
