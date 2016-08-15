
val PROGRAM_DURATION: Int = 3000
val BLINK_DURATION: Int = 500

fun blink() {
    led_set(LED_GREEN, true)
    led_set(LED_BLUE, true)
    led_set(LED_RED, true)
    led_set(LED_ORANGE, true)
    wait(BLINK_DURATION)
    led_set(LED_GREEN, false)
    led_set(LED_BLUE, false)
    led_set(LED_RED, false)
    led_set(LED_ORANGE, false)
}

fun simpleRoute() {
    while (true) {
        wait(PROGRAM_DURATION)
        blink()
        writeProto()
    }
}