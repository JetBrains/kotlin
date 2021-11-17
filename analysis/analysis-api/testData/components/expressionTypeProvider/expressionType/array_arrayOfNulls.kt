fun foo() {
    val arr = arrayOfNulls<List<*>>(10)
    <expr>arr</expr>[0] = emptyList<Any>()
}
