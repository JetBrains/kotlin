package sample

actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> {
    fun foo()
}

fun test() {
    useA {
        foo()
    }

    anotherUseA {
        <!UNRESOLVED_REFERENCE!>foo<!>()
    }

    anotherUseA {
        it.foo()
    }

    anotherUseA { a ->
        a.foo()
    }
}
