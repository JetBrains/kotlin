package net.server.handlers.main

import CodedInputStream
import McState
import SonarRequest
import SonarResponse
import control.Controller
import encodeProtoBuf
import net.server.handlers.AbstractHandler

class GetSonarData(val controller: Controller) : AbstractHandler() {
    val fromServerObjectBuilder = SonarRequest.BuilderSonarRequest(IntArray(0))
    val toServerObjectBuilder = SonarResponse.BuilderSonarResponse(IntArray(0))

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))

        if (McState.instance.isConnected()) {
            controller.executeRequestSensorData(message, callback)
            return
        }

        println("mc is disconnected!")
        val responseMessage = toServerObjectBuilder.build()
        callback.invoke(encodeProtoBuf(responseMessage))
        return
    }
}