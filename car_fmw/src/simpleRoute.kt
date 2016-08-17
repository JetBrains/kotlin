
fun simpleRoute() {
    while (true) {
        engine_forward()
        wait(PROGRAM_DURATION)
        blink()

        engine_turn_right()
        wait(PROGRAM_DURATION)
        blink()

        engine_backward()
        wait(PROGRAM_DURATION)
        blink()

        engine_turn_left()
        wait(PROGRAM_DURATION)
        blink()
    }
}