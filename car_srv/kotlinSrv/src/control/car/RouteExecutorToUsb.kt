package control.car

import RouteRequest
import control.RouteExecutor
import encodeProtoBuf
import mcTransport
import CodedOutputStream

class RouteExecutorToUsb : RouteExecutor {

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
}
