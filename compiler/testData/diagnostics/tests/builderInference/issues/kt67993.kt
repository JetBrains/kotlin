// FIR_IDENTICAL
// ISSUE: KT-67993
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// Reason: see KT-68031

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
