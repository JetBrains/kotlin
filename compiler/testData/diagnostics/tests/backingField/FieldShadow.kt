class My {
    // No initialization needed because no backing field
    val two: Int
        get() {
            val <!NAME_SHADOWING!>field<!> = 2
            return field
        }
}
