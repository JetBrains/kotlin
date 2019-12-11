class Foo(var x: Int?) {
    init {
        if (x != null) {
            val y = x
            // Error: x is not stable, Type(y) = Int?
            x.hashCode()
            y.hashCode()
            if (y == x) {
                // Still error
                y.hashCode()
            }
            if (x == null && y != x) {
                // Still error
                y.hashCode()
            }
        }
    }
}