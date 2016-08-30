@native
fun require(name: String): dynamic = noImpl

@native
fun setTimeout(callBack: () -> Unit, ms: Int): dynamic = noImpl

fun <T> encodeProtoBuf(protoMessage: T): ByteArray {
    val routeBytes: ByteArray
    when (protoMessage) {
        is LocationResponse -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }
        is UploadResult -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }
        is DebugResponseMemoryStats -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is DebugRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is RouteResponse -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is RouteRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }
        is RouteMetricRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is TaskRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is RouteDoneRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is SonarResponse -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        is SonarRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }
        is SonarExploreAngleRequest -> {
            val protoSize = protoMessage.getSizeNoTag()
            routeBytes = ByteArray(protoSize)
            val codedOutput = CodedOutputStream(routeBytes)
            protoMessage.writeTo(codedOutput)
        }

        else -> {
            println("PROTO MESSAGE DON'T ENCODE!")
            routeBytes = ByteArray(0)
        }

    }
    return routeBytes
}