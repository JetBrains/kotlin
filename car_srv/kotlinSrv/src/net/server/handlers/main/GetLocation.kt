package net.server.handlers.main

import CarState
import LocationResponse
import net.server.handlers.AbstractHandler

class GetLocation : AbstractHandler {

    val toServerObjectBuilder: LocationResponse.BuilderLocationResponse

    constructor() : super() {
        val defaultLocationData = LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()
        this.toServerObjectBuilder = LocationResponse.BuilderLocationResponse(defaultLocationData, 0)
    }

    override fun getBytesResponse(data: ByteArray, callback: (ByteArray) -> Unit) {
        val carState = CarState.instance
        val locationData = LocationResponse.LocationData.BuilderLocationData(carState.x.toInt(), carState.y.toInt(), carState.angle.toInt()).build()
        val responseMessage = toServerObjectBuilder.setLocationResponseData(locationData).build()
        callback.invoke(encodeProtoBuf(responseMessage))
    }
}