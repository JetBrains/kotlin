// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
class Foo(var x: Int?) {
    init {
        if (x != null) {
            val y = x
            // Error: x is not stable, Type(y) = Int?
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
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