
external fun leds_init()
external fun leds_clear_all()
external fun led_set(led: Int, on: Boolean)

val LED_GREEN = 0
val LED_ORANGE = 1
val LED_RED = 2
val LED_BLUE = 3

val BLINK_DURATION: Int = 1000

fun led_on() {
    led_set(LED_GREEN, true)
    led_set(LED_BLUE, true)
    led_set(LED_RED, true)
    led_set(LED_ORANGE, true)
}

fun led_off() {
    led_set(LED_GREEN, false)
    led_set(LED_BLUE, false)
    led_set(LED_RED, false)
    led_set(LED_ORANGE, false)
}

fun blink() {
    led_on()
    wait(BLINK_DURATION)
    led_off()
}
