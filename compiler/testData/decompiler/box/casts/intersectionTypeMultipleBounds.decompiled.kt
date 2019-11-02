interface A {
    abstract fun foo(): Any?
    abstract fun bar(): String
}

interface B {
    abstract fun foo(): String
}

fun <T : A, B> bar(x: T): String {
    if (x.foo().length != 2 || x.foo() != "OK") {
        return "fail 1"
    }
    if (x.bar() != "ok") {
        return "fail 2"
    }
    return "OK"
}

class C : A, B {
    override fun foo(): String {
        return "OK"
    }

    override fun bar(): String {
        return "ok"
    }

}

fun box(): String {
    return bar<C>(C())
}
