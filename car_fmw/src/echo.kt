fun echoUsb() {
    led_on()

    clear_buffer()
    while (true) {
        val command = receive_int()
        send_int(command)
    }

}

fun echoProto() {
    led_on()

    clear_buffer()
    while (true) {
        val route = readRoute()
        go(route)
    }
}

fun readRoute(): RouteRequest {
    val size = receive_int()
    val buffer = ByteArray(size)
    val stream = CodedInputStream(buffer)
    val result = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0)).parseFrom(stream).build()

    return result
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

//fun writeProto() {
//    val msg = DirectionResponse.BuilderDirectionResponse(4242).build()
//    val size = msg.getSizeNoTag()
//    val buffer = ByteArray(size)
//    val outputStream = CodedOutputStream(buffer)
//
//    msg.writeTo(outputStream)
//    sendByteArray(buffer)
//    send_int(0xDDEEFF)
//}