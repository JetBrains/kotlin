package net.server.handlers.main

import CodedInputStream
import McState
import RouteResponse
import control.Controller
import encodeProtoBuf
import net.server.handlers.AbstractHandler


class SetRouteMetric(val controller: Controller) : AbstractHandler() {
    val requestBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
    val responseBuilder = RouteResponse.BuilderRouteResponse(0)

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val message = requestBuilder.build()
        message.mergeFrom(CodedInputStream(data))

        if (McState.instance.isConnected()) {
            controller.executeMetricRoute(message, callback)
            return
        }

        println("mc is disconnected!")
        val responseMessage = responseBuilder.setCode(16).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }
}
