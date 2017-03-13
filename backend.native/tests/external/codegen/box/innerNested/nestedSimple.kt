class Outer {
    class Nested {
        fun box() = "OK"
    }
}

fun box() = Outer.Nested().box()
