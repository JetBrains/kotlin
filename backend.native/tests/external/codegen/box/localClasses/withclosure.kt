fun box(): String {
    val x = "OK"
    class Aaa {
        val y = x
    }

    return Aaa().y
}
