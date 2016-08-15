
fun echoUsb() {
    led_on()

    clear_buffer()
    while (true) {
        val command = receive_int();
        send_int(command)
    }

}