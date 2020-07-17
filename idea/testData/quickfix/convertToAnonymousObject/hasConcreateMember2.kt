// "Convert to anonymous object" "true"
interface I0 {
    fun x() {}
}

interface I : I0() {
    fun a()
    val b: Int
        get() = 1
}

fun foo(i: I) {}

fun test() {
    foo(<caret>I {})
}