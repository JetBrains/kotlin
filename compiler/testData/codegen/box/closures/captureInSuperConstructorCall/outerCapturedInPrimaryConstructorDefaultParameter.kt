class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String = { x.toString() }
    )
}

fun box() = Outer("OK").Inner().fn()
