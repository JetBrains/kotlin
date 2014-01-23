fun foo() {
    public trait T: A

    val a = object: A() {}

    fun bar() {
        val b = object: T {}
    }
}
