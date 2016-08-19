package net.server.handlers.main

import CodedInputStream
import MicroController
import RouteRequest
import RouteResponse
import control.RouteExecutor
import CodedOutputStream
import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/28/16.
 */
class SetRoute : AbstractHandler {

    val fromServerObjectBuilder: RouteRequest.BuilderRouteRequest
    val toServerObjectBuilder: RouteResponse.BuilderRouteResponse
    val routeExecutor: RouteExecutor

    constructor(routeExecutor: RouteExecutor) : super() {
        this.routeExecutor = routeExecutor
        this.fromServerObjectBuilder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
        this.toServerObjectBuilder = RouteResponse.BuilderRouteResponse(0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        println("set route handler")
        val car = MicroController.instance.car
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        if (!McState.instance.isConnected()) {
            println("mc is disconnected!")
            val responseMessage = toServerObjectBuilder.setCode(16).build()
            callback.invoke(encodeProtoBuf(responseMessage))
            return
        }
        routeExecutor.executeRoute(message)
        val responseMessage = toServerObjectBuilder.setCode(0).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }

    fun encodeProtoBuf(protoMessage: RouteResponse): ByteArray {
        val protoSize = protoMessage.getSizeNoTag()
        val routeBytes = ByteArray(protoSize)

        protoMessage.writeTo(CodedOutputStream(routeBytes))
        return routeBytes
    }

}