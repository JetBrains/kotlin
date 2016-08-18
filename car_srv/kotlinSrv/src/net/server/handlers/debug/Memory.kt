package net.server.handlers.debug

import CodedInputStream
import DebugRequest
import DebugResponseMemoryStats
import MicroController
import encodeProtoBuf
import mcTransport
import net.server.handlers.AbstractHandler

/**
 * Created by user on 8/18/16.
 */
class Memory : AbstractHandler {

    val fromServerObjectBuilder: DebugRequest.BuilderDebugRequest
    val toServerObjectBuilder: DebugResponseMemoryStats.BuilderDebugResponseMemoryStats

    constructor() : super() {
        fromServerObjectBuilder = DebugRequest.BuilderDebugRequest(DebugRequest.TYPE.MEMORYSTATS)
        toServerObjectBuilder = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(0, 0, 0, 0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {

        if (!MicroController.instance.isConnected()) {
            println("mc is disconnected!")
            val responseMessage = toServerObjectBuilder.build()
            callback.invoke(encodeProtoBuf(responseMessage))
            return
        }
        mcTransport.setCallBack { bytes ->
            callback.invoke(bytes)
        }
        mcTransport.sendBytes(data)
    }
}