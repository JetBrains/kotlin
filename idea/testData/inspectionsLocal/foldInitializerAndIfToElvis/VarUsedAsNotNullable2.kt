// PROBLEM: none
fun test(foo: Int?, bar: Int): Int {
    var i = foo
    <caret>if (i == null) {
        return bar
    }
    baz(i)
    val j = i + 1
    return j
}

fun baz(i: Int?) {}