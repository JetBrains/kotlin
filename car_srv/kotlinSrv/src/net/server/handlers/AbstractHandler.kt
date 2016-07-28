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
        js("resultMessage = {}")
        val afterExecute: () -> Unit = {
            if (this.protoEncoder != null) {
                val protoEn = this.protoEncoder//temporarily:)
                val resultMsg = resultMessage
                val resultBuffer = js("new protoEn(resultMsg)").encode()
                val trimBuffer = trimBuffer(resultBuffer.buffer, resultBuffer.limit)
                response.write(trimBuffer)
            }
            response.end()
        }
        makeResponse(message, resultMessage, afterExecute)
    }

    abstract fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit)

}