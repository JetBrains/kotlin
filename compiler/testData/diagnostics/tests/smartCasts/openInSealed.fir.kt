// IGNORE_REVERSED_RESOLVE
sealed class My(open val x: Int?) {
    init {
        if (x != null) {
            // Should be error: property is open
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
    }
}