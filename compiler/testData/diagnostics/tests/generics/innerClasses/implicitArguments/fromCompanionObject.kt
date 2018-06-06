// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
open class Outer<E> {
    inner class Inner<F>

}

class Derived : Outer<String>() {
    // Inner<Int> here means Outer<String>.Inner<Int>
    fun foo(x: Inner<Int>) {}
}

class A {
    companion object : Outer<String>()

    // Does not work, could be Outer<String>.Inner<Int>
    // TODO: Should work?
    fun foo(x: <!DEPRECATED_ACCESS_BY_SHORT_NAME, OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!><Int>) {
        // Inner<Char>() call use companion as implicit receiver
        val y: Outer<String>.Inner<Char> = Inner<Char>()
    }
}
