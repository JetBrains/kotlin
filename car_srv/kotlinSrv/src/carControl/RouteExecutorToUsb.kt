package carControl

import RouteRequest
import mcTransport
import CodedOutputStream

fun encode(i: Int): ByteArray {
    val result = ByteArray(4)
    result[0] = i.shr(24).toByte()
    result[1] = i.shr(16).toByte()
    result[2] = i.shr(8).toByte()
    result[3] = i.toByte()

    return result
}

fun decode(bytes: ByteArray): Int {
    var result = 0
    result += bytes[3]
    result += bytes[2].toInt().shl(8)
    result += bytes[1].toInt().shl(16)
    result += bytes[0].toInt().shl(24)
    return result
}

fun encodeRouteRequest(route: RouteRequest): ByteArray {
    val protoSize = route.getSizeNoTag()
    val routeBytes = ByteArray(protoSize)

    route.writeTo(CodedOutputStream(routeBytes))
    return routeBytes
}

class RouteExecutorToUsb : RouteExecutor {

    override fun executeRoute(route: RouteRequest) {
        println("Execute Route:")
        val size = encode(route.getSizeNoTag())
        val routeBytes = encodeRouteRequest(route)

        mcTransport.setCallBack { bytes ->
            println("Read $bytes; decoded: ${decode(bytes)}")
        }

        mcTransport.writeToFile(size)
        mcTransport.writeToFile(routeBytes)
    }
}
