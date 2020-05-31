fun test(foo: Int?, bar: Int): Int {
    var i = foo
    <caret>if (i == null) {
        return bar
    }
    return i
}