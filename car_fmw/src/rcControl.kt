
enum class RouteType(val id: Int) {
    FORWARD(0),
    BACKWARD(1),
    LEFT(2),
    RIGHT(3);
}

fun go(request: RouteRequest) {
    val times = request.distances
    val actions = request.angles
    var j = 0

    while (j < times.size) {
        val time = times[j]
        val action = actions[j]

        when (action) {
            RouteType.FORWARD.id -> engine_forward()
            RouteType.BACKWARD.id -> engine_backward()
            RouteType.LEFT.id -> engine_turn_left()
            RouteType.RIGHT.id -> engine_turn_right()
        }

        wait(time)
        engine_stop()
        j++
    }

}