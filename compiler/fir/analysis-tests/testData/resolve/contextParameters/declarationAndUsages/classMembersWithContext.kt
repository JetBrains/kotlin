// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
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
    fun usageInsideClass(){
        funMember()
        propertyMember
    }

    fun bar() {}
}

fun usageOutsideClass() {
    with(Base()) {
        with(A()) {
            funMember()
            propertyMember
        }
    }
}