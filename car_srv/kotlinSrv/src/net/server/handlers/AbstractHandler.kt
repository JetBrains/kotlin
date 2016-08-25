package net.server.handlers

abstract class AbstractHandler {
    fun execute(data: List<Byte>, response: dynamic) {
        val callBack = { resultBytes: ByteArray ->
            val resultBuffer = js("new Buffer(resultBytes)")
            response.write(resultBuffer)
            response.end()
        }

        try {
            getBytesResponse(data.toByteArray(), callBack)
        } catch (e: dynamic) {
            println("error in executing handler!")
            println(e.toString())
            callBack.invoke(ByteArray(0))
        }
    }

    abstract fun getBytesResponse(data: ByteArray, callback: (b: ByteArray) -> Unit)
}