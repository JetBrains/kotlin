
fun main() {
    /* init hardware */
    Time.init()
    Engine.init()
    Leds.init()
    Sonar.init()
    Connection.init()

    /* car task */
    Control.run()
}
