fun test(a: Any) {
    if (a !is Foo) {
        return
    }

    var result: Int = 0

    <expr>loop@</expr> while (true) {
        if (!a.process()) {
            break@loop
        }
    }
}

interface Foo {
    fun process(): Boolean
}