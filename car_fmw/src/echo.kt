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
        blink()
        go(route)
        blink()
        wait(PROGRAM_DURATION)
    }
}

fun readRoute(): RouteRequest {
    val buffer = receiveByteArray()
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

