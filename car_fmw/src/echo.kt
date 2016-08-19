
fun echoProto() {

    Memory.setHeap(Memory.DYNAMIC_HEAP)
    while (true) {
        val route = readRoute()
        Leds.blink()
        go(route)
        Leds.blink()
        Time.wait(1000)
        Memory.cleanDynamicHeap()
    }
}

fun readRoute(): RouteRequest {
    val buffer = Connection.receiveByteArray()
    val stream = CodedInputStream(buffer)
    val result = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)).parseFrom(stream).build()

    return result
}

fun writeRoute(route: RouteRequest) {
    val size = route.getSizeNoTag()
    val buffer = ByteArray(size)
    val stream = CodedOutputStream(buffer)

    route.writeTo(stream)

    Connection.sendByteArray(buffer)
}

