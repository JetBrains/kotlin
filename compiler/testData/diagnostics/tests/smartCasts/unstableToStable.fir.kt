class Foo(var x: Int?) {
    init {
        if (x != null) {
            val y = x
            // Error: x is not stable, Type(y) = Int?
            x<!UNSAFE_CALL!>.<!>hashCode()
            y<!UNSAFE_CALL!>.<!>hashCode()
            if (y == x) {
                // Still error
                y<!UNSAFE_CALL!>.<!>hashCode()
            }
            if (x == null && y != x) {
                // Still error
                y<!UNSAFE_CALL!>.<!>hashCode()
            }
        }
    }
}