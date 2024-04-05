class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String
    ) {
        constructor(
            unused: Int,
            fn: () -> String = { x.toString() }
        ) : this(fn)
    }
}

fun box() = Outer("OK").Inner(1).fn()
