package sample

actual interface A {
    fun foo()
}

fun test() {
    useA {
        foo()
    }

    anotherUseA {
        <!UNRESOLVED_REFERENCE("foo")!>foo<!>()
    }

    anotherUseA {
        it.foo()
    }

    anotherUseA { a ->
        a.foo()
    }
}
