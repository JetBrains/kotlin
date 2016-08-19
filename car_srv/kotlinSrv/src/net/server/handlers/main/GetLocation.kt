package net.server.handlers.main

import LocationResponse
import MicroController
import encodeProtoBuf
import net.server.handlers.AbstractHandler

/**
 * Created by user on 7/28/16.
 */
class GetLocation : AbstractHandler {

    val toServerObjectBuilder: LocationResponse.BuilderLocationResponse

    constructor() : super() {
        val defaultLocationData = LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()
        this.toServerObjectBuilder = LocationResponse.BuilderLocationResponse(defaultLocationData, 0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val car = MicroController.instance.car
        val locationData = LocationResponse.LocationData.BuilderLocationData(car.x.toInt(), car.y.toInt(), car.angle.toInt()).build()
        val responseMessage = toServerObjectBuilder.setLocationResponseData(locationData).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }
}