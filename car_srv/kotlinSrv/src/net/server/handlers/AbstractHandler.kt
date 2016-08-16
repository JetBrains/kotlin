package net.server.handlers

import trimBuffer

/**
 * Created by user on 7/27/16.
 */
abstract class AbstractHandler {

    fun execute(data: List<Byte>, response: dynamic) {
        getBytesResponse(data.toByteArray(), { resultBytes ->
            val resultBuffer = js("new Buffer(resultBytes)")
            response.write(resultBuffer)
            response.end()
        })
    }

    abstract fun getBytesResponse(data: ByteArray, callback: (b: ByteArray) -> Unit)

}