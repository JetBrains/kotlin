class Bar {
    fun bar() {}
}

class Foo(var x: Any) {
    init {
        if (x is Bar) {
            val y = x
            // Error: x is not stable, Type(y) = Any
            x.<!UNRESOLVED_REFERENCE!>bar<!>()
            y.<!UNRESOLVED_REFERENCE!>bar<!>()
            if (y == x) {
                // Still error
                y.<!UNRESOLVED_REFERENCE!>bar<!>()
            }
            if (x !is Bar && y != x) {
                // Still error
                y.<!UNRESOLVED_REFERENCE!>bar<!>()
            }
        }
    }
}