fun foo() {
    var x: Int
    fun bar() {
        x = 42
    }
    x.hashCode()
    bar()
}
