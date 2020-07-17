// "Convert to anonymous object" "true"
interface I {
    fun a()
    fun b() {}
}

fun foo(i: I) {}

fun test() {
    foo(<caret>I {})
}