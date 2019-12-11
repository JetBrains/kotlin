// PROBLEM: none
// WITH_RUNTIME
fun main() {
    val o = Foo("")
    o.run {
        val o2 = <caret>copy(prop = "New")
        bar(o2)
    }
}

data class Foo(val prop: String)

fun bar(foo: Foo) {}