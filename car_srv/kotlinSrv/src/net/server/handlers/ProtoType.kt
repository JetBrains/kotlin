package net.server.handlers

import McState
import TaskRequest
import mcTransport

class ProtoType : AbstractHandler {

    val fromServerObjectBuilder: TaskRequest.BuilderTaskRequest

    constructor() : super() {
        fromServerObjectBuilder = TaskRequest.BuilderTaskRequest(TaskRequest.TYPE.DEBUG)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        if (!McState.instance.isConnected()) {
            println("mc is disconnected!")
            callback.invoke(ByteArray(0))
            return
        }
        mcTransport.sendBytes(data)
        callback.invoke(ByteArray(0))
    }
}