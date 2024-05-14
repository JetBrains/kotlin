// ISSUE: KT-67993

class Builder<T> {
    var res: T? = null

    fun add(x: T) {
        res = x
    }
}

fun <T> foo(build: Builder<T>.() -> Unit): T {
    return Builder<T>().apply(build).res!!
}

class C {
    val a = foo {
        object {
            fun bar() {
                add(foo())
            }

            private fun foo() = "OK"
        }.bar()
    }
}

fun box(): String {
    return C().a
}
