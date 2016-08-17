fun echoUsb() {
    clear_buffer()
    while (true) {
        val command = receive_int()
        send_int(command)

        blink()
        wait(PROGRAM_DURATION)
    }

}

fun echoProto() {
    clear_buffer()

    while (true) {
        val route = readRoute()
        go(route)

        blink()
        wait(PROGRAM_DURATION)
    }
}

fun readRoute(): RouteRequest {
    val size = receive_int()
    val buffer = ByteArray(size)
    val stream = CodedInputStream(buffer)
    val result = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)).parseFrom(stream).build()

    return result
}

fun writeRoute(route: RouteRequest) {
    val size = route.getSizeNoTag()
    val buffer = ByteArray(size)
    val stream = CodedOutputStream(buffer)

    route.writeTo(stream)

    sendByteArray(buffer)
}

fun go(request: RouteRequest) {
    val times = request.distances
    var j = 0

    while (j < times.size) {
        val time = times[j]

        engine_forward()
        wait(time)
        engine_stop()
        j++
    }

}
