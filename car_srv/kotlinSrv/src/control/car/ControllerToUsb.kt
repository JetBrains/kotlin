package control.car

import RouteRequest
import control.Controller
import mcTransport

class ControllerToUsb : Controller {

    override fun executeRoute(route: RouteRequest, callBack: (ByteArray) -> Unit) {
        println("Execute Route:")

        mcTransport.setCallBack { bytes ->
            callBack.invoke(bytes)
        }

        mcTransport.sendProtoBuf(route)
    }

    override fun executeRequestSensorData(angles: IntArray, callBack: (ByteArray) -> Unit) {
        //todo make after connect sensor to car
    }
}