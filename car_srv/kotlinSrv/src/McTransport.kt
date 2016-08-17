/**
 * Created by user on 8/16/16.
 */

class McTransport() {

    private var writeStream: dynamic = null
    private var readStream: dynamic = null
    private val resultBytes = arrayListOf<Byte>()
    private var callback: (bytes: ByteArray) -> Unit = {}

    fun writeToFile(bytes: ByteArray) {
        println("write: " + bytes)
        val bytesT = bytes
        writeStream.write(js("new Buffer(bytesT)"));
//        writeStream.end();
    }

    fun writeToFile(byte: Byte) {
        writeToFile(ByteArray(1, { idx -> byte }))
    }

    fun initStreams(pathToFile: String) {
        writeStream = fs.createWriteStream(pathToFile);
        readStream = fs.createReadStream(pathToFile)
        readStream.on("readable", fun() {
            val data = readStream.read()
            if (data == null) {
                return
            }
            var messageLength = getBodyLength(resultBytes)

            for (i in 0..data.length - 1) {
                resultBytes.add(data[i])
                if (messageLength != -1 && messageLength + protoHeaderLength == resultBytes.size) {
                    callback.invoke(resultBytes.toByteArray())
                    resultBytes.clear()
                } else if (messageLength == -1) {
                    messageLength = getBodyLength(resultBytes)
                }
            }
        })
    }

    fun setCallBack(cb: (bytes: ByteArray) -> Unit) {
        this.callback = cb
    }

    fun closeStreams() {
        writeStream.end()
        writeStream = null
        readStream = null
    }

    private fun getBodyLength(resultBytes: List<Byte>): Int {
        if (resultBytes.size < protoHeaderLength) {
            //need first ${protoHeaderLength} bytes - header
            return -1
        }
        var res = 0
        for (i in 0..protoHeaderLength - 1) {
            val curByte = resultBytes.get(i)
            val curBytePositive: Int = if (curByte < 0) {
                256 + curByte
            } else {
                curByte.toInt()
            }
            res += curBytePositive * Math.pow(2.0, 24.0 - i * 8).toInt()
        }
        return res
    }

}

val protoHeaderLength: Int = 4
val mcTransport = McTransport()