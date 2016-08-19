
fun main() {
    /* init hardware */
    Time.init()
    Engine.init()
    Leds.init()
    Connection.init()

    /* car task */
    Control.run()
}
