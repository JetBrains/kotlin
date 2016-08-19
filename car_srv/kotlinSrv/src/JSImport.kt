@native
fun require(name: String): dynamic = noImpl

@native
fun setTimeout(callBack: () -> Unit, ms: Int): dynamic = noImpl

fun <T> encodeProtoBuf(protoMessage: T): ByteArray {
    val routeBytes: ByteArray
    println(protoMessage.toString())
    if (protoMessage is LocationResponse) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is UploadResult) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is DebugResponseMemoryStats) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    }else if (protoMessage is DebugRequest) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is DirectionResponse) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is RouteResponse) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is RouteRequest) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is TaskRequest) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else if (protoMessage is RouteDoneRequest) {
        val protoSize = protoMessage.getSizeNoTag()
        routeBytes = ByteArray(protoSize)
        val codedOutput = CodedOutputStream(routeBytes)
        protoMessage.writeTo(codedOutput)
    } else {
        println("PROTO MESSAGE DON'T ENCODE!")
        routeBytes = ByteArray(0)
    }
    return routeBytes
}