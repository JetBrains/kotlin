// "Convert to anonymous object" "true"
interface I {
    fun foo(a: String, b: Int): Int
}

fun test() {
    <caret>I {
        1
    }
}