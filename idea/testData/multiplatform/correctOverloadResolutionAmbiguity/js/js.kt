actual interface A<T> {
    actual fun foo(x: T)
    fun foo(x: String)
}

fun main() {
    bar().<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>("")
}
