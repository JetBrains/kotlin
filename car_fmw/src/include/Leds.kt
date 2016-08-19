
external fun car_leds_init()
external fun car_leds_clear_all()
external fun car_led_set(led: Int, on: Boolean)

object Leds {
    val BLINK_DURATION: Int = 1000
    val GREEN = 0
    val ORANGE = 1
    val RED = 2
    val BLUE = 3

    fun init() {
        car_leds_init()
    }

    fun set(led: Int, on: Boolean) {
        car_led_set(led, on)
    }

    fun switchAll(on: Boolean) {
        car_led_set(GREEN, on)
        car_led_set(BLUE, on)
        car_led_set(RED, on)
        car_led_set(ORANGE, on)
    }

    fun blink() {
        switchAll(true)
        Time.wait(BLINK_DURATION)
        switchAll(false)
    }
}



