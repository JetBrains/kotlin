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
            return<!LABEL_NAME_CLASH!>@foo<!> 1
        }
        return@foo 1
    }
}

fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}

