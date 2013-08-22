fun f() {
    foo {(): Int ->
        return@foo 1
    }
    foo({(): Int ->
        return@foo 1
    }
    )
    foo(a = {(): Int ->
        return@foo 1
    })

    foo {(): Int ->
        foo {
            (): Int ->
            return@foo 1
        }
        return@foo 1
    }
}

fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}

