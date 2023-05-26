// ISSUE: KT-37375

val foo: Any = Any()

fun bar() {
    operator fun Any.invoke(): String = ""

    fun baz() {
        operator fun Any.invoke(): Int = 1

        fun barbaz() {
            takeInt(<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>())
        }
    }
}

fun takeInt(x: Int) {}