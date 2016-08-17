fun foo(a: Int, b: Int) {}

fun noReorder1() = 1
fun noReorder2() = 2

fun reordered1() = 1
fun reordered2() = 2

fun test() {
    foo(a = noReorder1(), b = noReorder2())
    foo(b = reordered1(), a = reordered2())
}