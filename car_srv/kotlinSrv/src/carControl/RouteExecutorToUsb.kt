package carControl

import RouteRequest
import mcTransport
import encodeInt
import decodeInt
import encodeProtoBuf

class RouteExecutorToUsb : RouteExecutor {

    override fun executeRoute(route: RouteRequest) {
        println("Execute Route:")
        val size = encodeInt(route.getSizeNoTag())
        val routeBytes = encodeProtoBuf(route)

        mcTransport.setCallBack { bytes ->
            println("Read $bytes; decoded: ${decodeInt(bytes)}")
        }

        mcTransport.writeToFile(size)
        mcTransport.writeToFile(routeBytes)
    }
}
