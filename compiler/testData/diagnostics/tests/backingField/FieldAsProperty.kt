class My(val field: Int) {
    // Backing field, initializer
    val second: Int = 0
        get() = field

    // No backing field, no initializer
    val third: Int
        get() = this.field
}