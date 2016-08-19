external fun car_engine_init()

external fun car_engine_stop()
external fun car_engine_forward()
external fun car_engine_backward()
external fun car_engine_turn_left()
external fun car_engine_turn_right()

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
}