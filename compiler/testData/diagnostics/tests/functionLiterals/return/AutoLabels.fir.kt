fun f() {
    foo {
        return@foo 1
    }
    foo({
        return@foo 1
    }
    )
    foo(a = {
        return@foo 1
    })

    foo {
        foo {
            return@foo 1
        }
        return@foo 1
    }
}

fun foo(a: Any) {}

