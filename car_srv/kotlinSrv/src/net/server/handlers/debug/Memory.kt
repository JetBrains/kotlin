package net.server.handlers.debug

import DebugRequest
import DebugResponseMemoryStats
import McState
import mcTransport
import net.server.handlers.AbstractHandler

class Memory : AbstractHandler {

    val fromServerObjectBuilder: DebugRequest.BuilderDebugRequest
    val toServerObjectBuilder: DebugResponseMemoryStats.BuilderDebugResponseMemoryStats

    constructor() : super() {
        fromServerObjectBuilder = DebugRequest.BuilderDebugRequest(DebugRequest.TYPE.MEMORYSTATS)
        toServerObjectBuilder = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(0, 0, 0, 0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {

        if (!McState.instance.isConnected()) {
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