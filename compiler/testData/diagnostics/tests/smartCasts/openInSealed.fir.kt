sealed class My(open val x: Int?) {
    init {
        if (x != null) {
            // Should be error: property is open
            x.hashCode()
        }
    }
}