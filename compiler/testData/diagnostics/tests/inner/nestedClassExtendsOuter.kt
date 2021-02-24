// FIR_IDENTICAL
open class Outer {
    class Nested : Outer() {
        fun bar() = foo()
        fun baz() = super.foo()
    }
    
    fun foo() = 42
}
