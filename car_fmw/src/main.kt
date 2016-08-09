external fun time_init()

external fun leds_init()

external fun engine_init()
external fun engine_stop()
external fun engine_forward()
external fun engine_backward()
external fun engine_turn_left()
external fun engine_turn_right()

external fun user_brn_init(i: () -> Unit)
external fun run_programmed_car(i: () -> Unit)
external fun run_rc_car(i: () -> Unit)

external fun VCP_init()

external fun wait(i: Int)

val CAR_MODE_PROGRAMMED: Int = 0
val CAR_MODE_REMOTE_CONTROL: Int = 1
val CAR_MODE_LAST: Int = 2

val PROGRAM_DURATION: Int = 3000

annotation class Native(val type: String = "")

@Native
class MyClass(@Native("i32") val i: Int, @Native("i16") val j: Int, @Native("i8") val k: Int)

fun engine_program() {
    engine_init()

    while (2 < 3) {
        engine_forward()
        wait(PROGRAM_DURATION)
        engine_stop()

        engine_backward()
        wait(PROGRAM_DURATION)
        engine_stop()

        engine_turn_right()
        wait(PROGRAM_DURATION)
        engine_stop()

        engine_turn_right()
        wait(PROGRAM_DURATION)
        engine_stop()
    }
}

fun kotlin_main() {
    time_init()
    engine_program()
}
