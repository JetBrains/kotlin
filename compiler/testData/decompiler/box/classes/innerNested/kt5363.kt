class Outer {
    class Nested{
        fun foo(s: String) = s.extension()
    }

    companion object {
        private fun String.extension(): String = this
    }
}

fun box(): String {
    return Outer.Nested().foo("OK")
}