class Outer {
    class Nested
    inner class Inner
    
    fun Inner.foo() {
        Outer()
        Nested()
        Inner()
    }
    
    fun Nested.bar() {
        Outer()
        Nested()
        Inner()
    }
    
    fun Outer.baz() {
        Outer()
        Nested()
        Inner()
    }
}

fun Outer.foo() {
    Outer()
    <!UNRESOLVED_REFERENCE!>Nested<!>()
    Inner()
}
