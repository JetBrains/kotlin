// FIX: none
// WITH_RUNTIME
fun main() {
    val o = Foo("")
    o.run {
        <caret>copy(prop = "New")
        bar(o)
    }
}

data class Foo(val prop: String)

fun bar(foo: Foo) {}