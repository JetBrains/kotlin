package control

import RouteRequest

interface Controller {

    fun executeRoute(route: RouteRequest, callBack: (ByteArray) -> Unit)

    fun executeRequestSensorData(angles: IntArray, callBack: (ByteArray) -> Unit)

}