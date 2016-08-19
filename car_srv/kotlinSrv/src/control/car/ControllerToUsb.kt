package control.car

import RouteRequest
import control.Controller
import mcTransport

class ControllerToUsb : Controller {

    override fun executeRoute(route: RouteRequest) {
        println("Execute Route:")

        mcTransport.setCallBack { bytes ->
            println("Read $bytes;")
        }

        mcTransport.sendProtoBuf(route)
    }

    override fun getSensorData(degrees: IntArray): IntArray {
        return IntArray(0)//todo make after connect sensor to car
    }
}