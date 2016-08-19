
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
            RouteType.FORWARD.id -> Engine.forward()
            RouteType.BACKWARD.id -> Engine.backward()
            RouteType.LEFT.id -> Engine.left()
            RouteType.RIGHT.id -> Engine.right()
        }

        Time.wait(time)
        Engine.stop()
        j++
    }

}