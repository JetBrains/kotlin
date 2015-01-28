// IS_APPLICABLE: false
class A(var n: Int) {
    var <caret>foo: Int
        get() = n + 1
        set(value: Int) { n = value - 1 }
}

fun test() {
    val t = A(1).foo
}