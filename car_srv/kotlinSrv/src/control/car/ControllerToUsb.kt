package control.car

import RouteRequest
import control.Controller
import mcTransport
import SonarRequest

class ControllerToUsb : Controller {

    override fun executeRoute(route: RouteRequest, callBack: (ByteArray) -> Unit) {
        println("Execute Route:")

        mcTransport.setCallBack { bytes ->
            callBack.invoke(bytes)
        }

        mcTransport.sendProtoBuf(route)
    }

    override fun executeRequestSensorData(sonarRequest: SonarRequest, callBack: (ByteArray) -> Unit) {
        println("sonar data")
        mcTransport.setCallBack { bytes ->
            callBack.invoke(bytes)
        }
        mcTransport.sendProtoBuf(sonarRequest)
    }
}