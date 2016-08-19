package control

import RouteRequest

interface Controller {

    fun executeRoute(route: RouteRequest)

    fun getSensorData(degrees: IntArray): IntArray

}