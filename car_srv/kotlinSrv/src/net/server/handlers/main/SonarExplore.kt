package net.server.handlers.main

import CodedInputStream
import McState
import SonarExploreAngleRequest
import SonarExploreAngleResponse
import control.Controller
import encodeProtoBuf
import net.server.handlers.AbstractHandler

class SonarExplore(val controller: Controller) : AbstractHandler() {
    val request = SonarExploreAngleRequest.BuilderSonarExploreAngleRequest(0, 0)
    val response = SonarExploreAngleResponse.BuilderSonarExploreAngleResponse(IntArray(0))

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = request.build()
        message.mergeFrom(CodedInputStream(data))

        if (McState.instance.isConnected()) {
            controller.executeRequestSensorExploreData(message, callback)
            return
        }

        println("mc is disconnected!")
        val responseMessage = response.build()
        callback.invoke(encodeProtoBuf(responseMessage))
        return
    }
}
