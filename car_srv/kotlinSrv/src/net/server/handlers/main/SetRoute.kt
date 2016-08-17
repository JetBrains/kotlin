package net.server.handlers.main

import net.server.handlers.AbstractHandler
import CodedInputStream
import encodeProtoBuf

/**
 * Created by user on 7/28/16.
 */
class SetRoute : AbstractHandler {

    val fromServerObjectBuilder: RouteRequest.BuilderRouteRequest
    val toServerObjectBuilder: RouteResponse.BuilderRouteResponse

    constructor(fromSrv: RouteRequest.BuilderRouteRequest, toSrv: RouteResponse.BuilderRouteResponse) : super() {
        this.fromServerObjectBuilder = fromSrv
        this.toServerObjectBuilder = toSrv
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val car = MicroController.instance.car
        val message = fromServerObjectBuilder.build()
        message.mergeFrom(CodedInputStream(data))
        car.routeExecutor.executeRoute(message)
        val responseMessage = toServerObjectBuilder.setCode(0).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }

}