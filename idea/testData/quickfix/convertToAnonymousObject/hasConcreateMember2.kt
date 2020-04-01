// "Convert to anonymous object" "true"
interface I {
    fun a()
    val b: Int
        get() = 1
}

fun foo(i: I) {}

fun test() {
    foo(<caret>I {})
}