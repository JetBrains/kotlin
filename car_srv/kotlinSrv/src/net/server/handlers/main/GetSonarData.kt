package net.server.handlers.main

import CodedInputStream
import McState
import SonarRequest
import SonarResponse
import control.Controller
import encodeProtoBuf
import net.server.handlers.AbstractHandler

class GetSonarData : AbstractHandler {

    val fromServerObjectBuilder: SonarRequest.BuilderSonarRequest
    val toServerObjectBuilder: SonarResponse.BuilderSonarResponse
    val controller: Controller

    constructor(routeExecutor: Controller) : super() {
        this.controller = routeExecutor
        this.fromServerObjectBuilder = SonarRequest.BuilderSonarRequest(IntArray(0))
        this.toServerObjectBuilder = SonarResponse.BuilderSonarResponse(IntArray(0))
    }


    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        if (!McState.instance.isConnected()) {
            println("mc is disconnected!")
            val responseMessage = toServerObjectBuilder.build()
            callback.invoke(encodeProtoBuf(responseMessage))
            return
        }
        controller.executeRequestSensorData(message.angles, callback)
    }
}