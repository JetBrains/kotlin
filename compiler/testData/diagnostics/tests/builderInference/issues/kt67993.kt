// FIR_IDENTICAL
// ISSUE: KT-67993

class Builder<T> {
    fun add(x: T) {}
}

fun <T> foo(build: Builder<T>.() -> Unit) {
    Builder<T>().apply(build)
}

class C {
    val a = foo {
        object {
            fun bar() {
                add(foo())
            }

            private fun foo() = "..."
        }
    }
}
