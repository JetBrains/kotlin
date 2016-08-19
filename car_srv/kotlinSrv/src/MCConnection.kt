class McTransport() : MCConnectObserver<String> {

    private var writeStream: dynamic = null
    private var readStream: dynamic = null
    private val resultBytes = arrayListOf<Byte>()
    private var callback: (bytes: ByteArray) -> Unit = {}

    init {
        McConditionMonitor.instance.addObserver(this)
    }

    private fun sendBytes(bytes: ByteArray) {
        val bytesHeader = encodeInt(bytes.size)
        println("write: $bytesHeader")
        writeStream.write(js("new Buffer(bytesHeader)"))
        println("write: $bytes")
        writeStream.write(js("new Buffer(bytes)"))
    }


    fun sendProtoBuf(message: DebugRequest) {
        val typeMessage = getProtoBufTypeMessage(TaskRequest.Type.DEBUG)
        sendProtoBufType(typeMessage)
        sendBytes(encodeProtoBuf(message))
    }

    fun sendProtoBuf(message: RouteRequest) {
        val typeMessage = getProtoBufTypeMessage(TaskRequest.Type.ROUTE)
        sendProtoBufType(typeMessage)
        sendBytes(encodeProtoBuf(message))
    }

    private fun sendProtoBufType(messageType: TaskRequest) {
        val typeBytes = encodeProtoBuf(messageType)
        sendBytes(typeBytes)
    }

    private fun getProtoBufTypeMessage(type: TaskRequest.Type): TaskRequest {
        val result = TaskRequest.BuilderTaskRequest(type)
        return result.build()
    }

    private fun initStreams(pathToFile: String) {
        writeStream = fs.createWriteStream(pathToFile)
        readStream = fs.createReadStream(pathToFile)
        readStream.on("readable", fun() {
            val data = readStream.read() ?: return
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

    override fun connect(transportFileName: String) {
        initStreams(transportFileName)
    }

    override fun disconnect() {
        closeStreams()
    }

    fun setCallBack(cb: (bytes: ByteArray) -> Unit) {
        this.callback = cb
    }

    private fun closeStreams() {
        writeStream.end()
        writeStream = null
        readStream = null
    }

    private fun getBodyLength(resultBytes: List<Byte>): Int {
        if (resultBytes.size < protoHeaderLength) {
            //need first ${protoHeaderLength} bytes - header
            return -1
        }
        return decodeInt(resultBytes.toByteArray())
    }

    private fun encodeInt(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = i.shr(24).toByte()
        result[1] = i.shr(16).toByte()
        result[2] = i.shr(8).toByte()
        result[3] = i.toByte()

        return result
    }

    private fun decodeInt(bytes: ByteArray): Int {
        var result = 0
        result += bytes[3]
        result += bytes[2].toInt().shl(8)
        result += bytes[1].toInt().shl(16)
        result += bytes[0].toInt().shl(24)
        return result
    }
}

private val protoHeaderLength: Int = 4
val mcTransport = McTransport()