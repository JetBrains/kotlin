package control.car

import RouteMetricRequest
import RouteRequest
import SonarRequest
import control.Controller
import mcTransport

class ControllerToUsb : Controller {
    override fun executeRoute(route: RouteRequest, callback: (ByteArray) -> Unit) {
        println("Execute Route:")

        mcTransport.setCallBack { bytes ->
            callback.invoke(bytes)
        }

        mcTransport.sendProtoBuf(route)
    }

    override fun executeMetricRoute(request: RouteMetricRequest, callback: (ByteArray) -> Unit) {
        println("Execute Route:")

        mcTransport.setCallBack { bytes ->
            callback.invoke(bytes)
        }

        mcTransport.sendProtoBuf(request)
    }

    override fun executeRequestSensorData(sonarRequest: SonarRequest, callback: (ByteArray) -> Unit) {
        println("sonar data")
        mcTransport.setCallBack { bytes ->
            callback.invoke(bytes)
        }
        mcTransport.sendProtoBuf(sonarRequest)
    }
}