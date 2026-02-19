class A {
    fun foo(a: String): String = a
}

class Base {
    context(a: A)
    val propertyMember: String
        get() = a.foo("str")

    context(a: A)
    fun usageInsideClass() {
        <expr>propertyMember</expr>
    }
}

// LANGUAGE: +ContextParameters
