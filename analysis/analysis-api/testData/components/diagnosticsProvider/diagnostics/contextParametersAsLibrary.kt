// IGNORE_FE10
// LANGUAGE: +ContextParameters
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MyDependency.kt
class A {
    fun foo(a: String): String {
        return a
    }
}

class Base {
    context(a: A)
    fun funMember() {
        this.bar()
        a.foo("")
    }

    context(a: A)
    val propertyMember: String
        get() {
            this.bar()
            return a.foo("")
        }

    context(a: A)
    fun usageInsideClass() {
        funMember()
        propertyMember
    }

    fun bar() {}
}

// MODULE: main(lib)
// FILE: main.kt
fun usageOutsideClass() {
    with(Base()) {
        with(A()) {
            funMember()
            propertyMember
        }

        funMember()
        propertyMember
    }
}
