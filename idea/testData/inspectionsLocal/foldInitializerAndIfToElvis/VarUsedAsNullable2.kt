fun test(foo: Int?, bar: Int): Int {
    var i = foo
    <caret>if (i == null) {
        return bar
    }
    return baz(i)
}

fun baz(i: Int?) = 1