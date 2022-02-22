fun Int.with() {
    with("") {
        this<!LABEL_RESOLVE_WILL_CHANGE("function with; anonymous function")!>@with<!>.inc()
    }
}

fun Int.bar() {
    with("") bar@{
        this<!LABEL_RESOLVE_WILL_CHANGE!>@bar<!>.inc()
    }
}

fun foo(f: with.() -> Unit) {}

class with {
    fun foo() {
        with("") {
            this<!LABEL_RESOLVE_WILL_CHANGE("class with; anonymous function")!>@with<!>.foo()
        }

        with("") with@{
            this<!LABEL_RESOLVE_WILL_CHANGE!>@with<!>.foo()
        }

        with("") other@{
            this@with.foo()
        }
    }
}

private typealias Extension = TypedThis

class TypedThis {
    fun TypedThis.baz() {
        this@TypedThis
    }

    fun Extension.bar() {
        this@TypedThis
    }
}
