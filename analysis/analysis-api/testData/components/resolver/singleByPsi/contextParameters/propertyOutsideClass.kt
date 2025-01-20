class A {
    fun foo(a: String): String = a
}

class Base {
    context(a: A)
    val propertyMember: String
        get() {
            return a.foo("")
        }
}

fun usageOutsideClass() {
    with(Base()) {
        with(A()) {
            <expr>propertyMember</expr>
            1
        }
    }
}

// LANGUAGE: +ContextParameters
