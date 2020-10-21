class Foo(var x: Int?) {
    init {
        if (x != null) {
            val y = x
            // Error: x is not stable, Type(y) = Int?
            x.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
            y.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
            if (y == x) {
                // Still error
                y.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
            }
            if (x == null && y != x) {
                // Still error
                y.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
            }
        }
    }
}