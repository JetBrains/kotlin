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

    fun drive(direction: Int) {
        when (direction) {
            RouteType.FORWARD.id -> forward()
            RouteType.BACKWARD.id -> backward()
            RouteType.LEFT.id -> left()
            RouteType.RIGHT.id -> right()
        }
    }
}