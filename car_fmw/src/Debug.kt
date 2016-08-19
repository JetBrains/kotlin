
object Debug {
    var PROGRAM_DURATION = 1000

    fun echoUsbTest() {
        val command = Connection.receiveInt()
        Connection.sendInt(command)
        Time.wait(PROGRAM_DURATION)
    }

    fun simpleRouteTest() {
        Engine.forward()
        Leds.blink()
        Time.wait(PROGRAM_DURATION)

        Engine.right()
        Leds.blink()
        Time.wait(PROGRAM_DURATION)

        Engine.backward()
        Leds.blink()
        Time.wait(PROGRAM_DURATION)

        Engine.left()
        Leds.blink()
        Time.wait(PROGRAM_DURATION)
    }

    fun blinkTest() {
        Leds.blink()
        Time.wait(PROGRAM_DURATION)
    }
}