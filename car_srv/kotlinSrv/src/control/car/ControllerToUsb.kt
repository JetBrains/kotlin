package control.car

import CodedOutputStream
import RouteRequest
import control.Controller
import mcTransport

class ControllerToUsb : Controller {

    override fun executeRoute(route: RouteRequest) {
        println("Execute Route:")
        val protoSize = route.getSizeNoTag()
        val routeBytes = ByteArray(protoSize)

        route.writeTo(CodedOutputStream(routeBytes))

        mcTransport.setCallBack { bytes ->
            println("Read $bytes;")
        }

        mcTransport.sendBytes(routeBytes)
    }

    override fun getSensorData(degrees: IntArray): IntArray {
        return IntArray(0)//todo make after connect sensor to car
    }
}