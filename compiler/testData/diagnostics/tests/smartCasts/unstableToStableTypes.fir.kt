class Bar {
    fun bar() {}
}

class Foo(var x: Any) {
    init {
        if (x is Bar) {
            val y = x
            // Error: x is not stable, Type(y) = Any
            x.bar()
            y.bar()
            if (y == x) {
                // Still error
                y.bar()
            }
            if (x !is Bar && y != x) {
                // Still error
                y.bar()
            }
        }
    }
}