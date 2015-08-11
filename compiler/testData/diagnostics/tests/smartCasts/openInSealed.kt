sealed class My(open val x: Int?) {
    init {
        if (<!DEBUG_INFO_LEAKING_THIS!>x<!> != null) {
            // Should be error: property is open
            <!SMARTCAST_IMPOSSIBLE, DEBUG_INFO_LEAKING_THIS!>x<!>.hashCode()
        }
    }
}