class Outer {
    class Nested {
        fun fn() = s
    }

    companion object {
        private val s = "OK"
    }
}

fun box(): String {
    return Outer.Nested().fn()
}