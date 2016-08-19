package control

import RouteRequest

interface Controller {

    fun executeRoute(route: RouteRequest, callBack: (ByteArray) -> Unit)

    fun executeRequestSensorData(degrees: IntArray, callBack: (ByteArray) -> Unit)

}