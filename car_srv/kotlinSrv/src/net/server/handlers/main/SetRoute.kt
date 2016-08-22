package net.server.handlers.main

import CodedInputStream
import McState
import RouteRequest
import RouteResponse
import control.Controller
import encodeProtoBuf
import net.server.handlers.AbstractHandler

class SetRoute : AbstractHandler {

    val fromServerObjectBuilder: RouteRequest.BuilderRouteRequest
    val toServerObjectBuilder: RouteResponse.BuilderRouteResponse
    val controller: Controller

    constructor(routeExecutor: Controller) : super() {
        this.controller = routeExecutor
        this.fromServerObjectBuilder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
        this.toServerObjectBuilder = RouteResponse.BuilderRouteResponse(0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        if (!McState.instance.isConnected()) {
            println("mc is disconnected!")
            val responseMessage = toServerObjectBuilder.setCode(16).build()
            callback.invoke(encodeProtoBuf(responseMessage))
            return
        }
        controller.executeRoute(message, callback)
    }
}