// FIR_IDENTICAL

object A {
    class B
    fun foo() = 1
    object Bar {}
}

fun <T> test(a: T) {
    val c = (a as A)
    // TODO: report "nested class accessed via instance reference"
    c.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: B">B</error>()
}
