external fun car_engine_init()

external fun car_engine_stop()
external fun car_engine_forward()
external fun car_engine_backward()
external fun car_engine_turn_left()
external fun car_engine_turn_right()

enum class RouteType(val id: Int) {
    FORWARD(0),
    BACKWARD(1),
    LEFT(2),
    RIGHT(3);
}

object Engine {
    val VELOCITY_DRIVE: Double = 0.05 // centimeter in millisecond
    val VEL0CITY_ROTATE: Double = 0.05 // degree in millisecond

    fun init() {
        car_engine_init()
    }

    fun stop() {
        car_engine_stop()
    }

    fun forward() {
        car_engine_forward()
    }

    fun backward() {
        car_engine_backward()
    }

    fun left() {
        car_engine_turn_left()
    }

    fun right() {
        car_engine_turn_right()
    }

    fun go(direction: Int) {
        when (direction) {
            RouteType.FORWARD.id -> forward()
            RouteType.BACKWARD.id -> backward()
            RouteType.LEFT.id -> left()
            RouteType.RIGHT.id -> right()
        }
    }

    fun drive(direction: Int, distance: Int) {
        val duration = when (direction) {
            RouteType.FORWARD.id,
            RouteType.BACKWARD.id -> (distance.toDouble() / VELOCITY_DRIVE).toInt()
            RouteType.LEFT.id,
            RouteType.RIGHT.id -> (distance.toDouble() / VEL0CITY_ROTATE).toInt()
            else -> 0
        }

        go(direction)
        Time.wait(duration)
        stop()
    }
}