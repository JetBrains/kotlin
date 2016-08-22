package control

import RouteRequest
import SonarRequest

interface Controller {

    fun executeRoute(route: RouteRequest, callBack: (ByteArray) -> Unit)

    fun executeRequestSensorData(sonarRequest: SonarRequest, callBack: (ByteArray) -> Unit)

}