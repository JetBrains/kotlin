// PROBLEM: none
// WITH_RUNTIME
fun main() {
    val o = Foo("")
    val o2 = o.<caret>copy(prop = "New")
    bar(o2)
}

data class Foo(val prop: String)

fun bar(foo: Foo) {}