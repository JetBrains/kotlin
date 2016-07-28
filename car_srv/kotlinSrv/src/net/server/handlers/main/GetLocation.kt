package net.server.handlers.main

import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/28/16.
 */
class GetLocation : AbstractHandler {

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder)

    override fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit) {
        val car = MicroController.instance.car
        val locationResponseData = {}
        js("locationResponseData = {x:car.x, y:car.y, angle:car.angle}")
        responseMessage.locationResponseData = locationResponseData
        finalCallback.invoke()
    }
}