sealed class My(open val x: Int?) {
    init {
        if (x != null) {
            // Should be error: property is open
            x.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
        }
    }
}