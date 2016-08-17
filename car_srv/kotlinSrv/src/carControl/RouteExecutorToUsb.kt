package carControl

import RouteRequest
import mcTransport
import CodedOutputStream

/**
 * Created by user on 8/17/16.
 */
class RouteExecutorToUsb : RouteExecutor {

    override fun executeRoute(route: RouteRequest) {

        val protoSize = route.getSizeNoTag()
        val routeBytes = ByteArray(protoSize)
        route.writeTo(CodedOutputStream(routeBytes))

        val fullBytes = ByteArray(route.getSizeNoTag() + 4)

        for (i in 0..routeBytes.size - 1) {
            fullBytes[i + 4] = routeBytes[i]
        }
        fullBytes[0] = protoSize.shr(24).toByte()
        fullBytes[1] = protoSize.shr(16).toByte()
        fullBytes[2] = protoSize.shr(8).toByte()
        fullBytes[3] = protoSize.toByte()

        mcTransport.writeToFile(fullBytes)
    }
}