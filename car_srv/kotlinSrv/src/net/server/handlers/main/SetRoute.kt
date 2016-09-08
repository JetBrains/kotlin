package net.server.handlers.main

import CodedInputStream
import McState
import RouteRequest
import RouteResponse
import control.Controller
import encodeProtoBuf
import net.server.handlers.AbstractHandler

class SetRoute(var controller: Controller) : AbstractHandler() {
    val requestBuilder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
    val responseBuilder = RouteResponse.BuilderRouteResponse(0)

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = requestBuilder.build()
        message.mergeFrom(CodedInputStream(data))

        if (McState.instance.isConnected()) {
            controller.executeRoute(message, callback)
            return
        }

        println("mc is disconnected!")
        val responseMessage = responseBuilder.setCode(16).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }
}