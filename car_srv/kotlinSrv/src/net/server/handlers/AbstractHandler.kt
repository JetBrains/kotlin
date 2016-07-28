package server.handlers

/**
 * Created by user on 7/27/16.
 */
abstract class AbstractHandler(protoDecoder: dynamic, protoEncoder: dynamic) {

    val protoDecoder: dynamic
    val protoEncoder: dynamic

    init {
        this.protoDecoder = protoDecoder
        this.protoEncoder = protoEncoder
    }


    fun execute(data: List<Byte>, response: dynamic) {

        val message = protoDecoder.decode(data.toByteArray())
        val resultMessage: dynamic = {}
        js("resultMessage = {}")//todo bad?
        val afterExecute: () -> Unit = {
            val protoEn = this.protoEncoder//temporarily:)
            val resultMsg = resultMessage
            val resultObject = js("new protoEn(resultMsg)")

            val resultBuffer = resultObject.encode()
            val byteArray = ByteArray(resultBuffer.limit);
            for (i in 0..resultBuffer.limit - 1) {
                byteArray[i] = resultBuffer.buffer[i]
            }
            response.write(js("new Buffer(byteArray)"))
            response.end()
        }
        makeResult(message, resultMessage, afterExecute)
    }

    abstract fun makeResult(message: dynamic, resultMessage: dynamic, finalCallback: () -> Unit)

}