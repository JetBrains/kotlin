package net.server.handlers.main

import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/28/16.
 */
class SetRoute : AbstractHandler {

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder)

    override fun makeResponse(message: dynamic, responseMessage: dynamic, finalCallback: () -> Unit) {
        val car = MicroController.instance.car

        car.routeExecutor.executeRoute(message)

        responseMessage.code = 0
        responseMessage.errorMsg = ""

        finalCallback.invoke()
    }
}