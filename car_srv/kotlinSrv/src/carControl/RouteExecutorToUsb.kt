package carControl

import RouteRequest
import mcTransport
import encodeInt
import decodeInt
import encodeProtoBuf

class RouteExecutorToUsb : RouteExecutor {

    override fun executeRoute(route: RouteRequest) {
        println("Execute Route:")
        val routeBytes = encodeProtoBuf(route)

        mcTransport.setCallBack { bytes ->
            println("Read $bytes;")
        }

        mcTransport.sendBytes(routeBytes)
    }
}
