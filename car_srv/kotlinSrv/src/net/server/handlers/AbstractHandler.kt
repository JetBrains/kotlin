package net.server.handlers

import trimBuffer

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


        val message = if (protoDecoder != null) protoDecoder.decode(data.toByteArray()) else null
        val resultMessage: dynamic = {}
        js("resultMessage = {}")//todo bad?
        val afterExecute: () -> Unit = {
            if (this.protoEncoder != null) {
                val protoEn = this.protoEncoder//temporarily:)
                val resultMsg = resultMessage
                val resultObject = js("new protoEn(resultMsg)")
                val resultBuffer = resultObject.encode()
                response.write(trimBuffer(resultBuffer.buffer, resultBuffer.limit))
            }
            response.end()
        }
        makeResponse(message, resultMessage, afterExecute)
    }

    abstract fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit)

}