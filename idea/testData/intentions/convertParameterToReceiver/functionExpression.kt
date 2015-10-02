interface T {
    val foo: Int
}

val f = fun(<caret>t: T): Int {
    return t.foo
}