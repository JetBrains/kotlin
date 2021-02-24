class Outer {
    val x = "O"
    inner class Inner {
        val y = x + "K"
    }
}

fun box() = Outer().Inner().y