fun foo() {
    val arr = arrayOfNulls<List<*>>(10)
    <expr>arr[0]</expr> = emptyList<Any>()
}