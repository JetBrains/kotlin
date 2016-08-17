package net.server.handlers.main

import net.server.handlers.AbstractHandler
import encodeProtoBuf

/**
 * Created by user on 7/28/16.
 */
class GetLocation : AbstractHandler {

    val toServerObjectBuilder: LocationResponse.BuilderLocationResponse

    constructor(toSrv: LocationResponse.BuilderLocationResponse) : super() {
        this.toServerObjectBuilder = toSrv
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val car = MicroController.instance.car
        val locationData = LocationResponse.LocationData.BuilderLocationData(car.x.toInt(), car.y.toInt(), car.angle.toInt()).build()
        val responseMessage = toServerObjectBuilder.setLocationResponseData(locationData).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }
}