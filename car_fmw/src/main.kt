
fun main() {
    /* init hardware */
    Time.init()
    Engine.init()
    Leds.init()
    Sonar.init()
    Connection.init()

    Leds.set(Leds.GREEN, true)

    /* car task */
//    Control.run()

    /* Voyager */
    Voyager.run()
}
