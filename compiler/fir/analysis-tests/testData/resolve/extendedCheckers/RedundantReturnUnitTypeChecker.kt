data class My(val x: Unit)

interface I {
    val x: Unit
}

class A {
    fun too(): @<!NOT_AN_ANNOTATION_CLASS!>Annotation<!> Unit {}

    fun foo(): <!REDUNDANT_RETURN_UNIT_TYPE!>Unit<!>
    {
    }

    fun bar(): <!REDUNDANT_RETURN_UNIT_TYPE!>Unit<!>
    {
        return Unit
    }

    fun baz(): Unit = bar()

    fun f1(): Int = 1

    fun f2(): <!REDUNDANT_RETURN_UNIT_TYPE!>Unit<!>
    {
        throw UnsupportedOperationException("")
    }

    fun f3(): Unit = throw UnsupportedOperationException("")
}

class B {
    fun <T> run(f: () -> T) = f()

    fun foo(): Unit = run {
        bar()
    }

    fun bar() = 1

    fun call(f: () -> Unit) = f()

    fun boo(): Unit = call {
        baz()
    }

    fun baz() {}

    fun <T, R> T.let(f: (T) -> R) = f(this)

    fun goo(): Unit = 1.let {
        bar()
    }
}

