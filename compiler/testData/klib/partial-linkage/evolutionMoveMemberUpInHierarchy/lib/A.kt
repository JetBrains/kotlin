open class Y: X() {
    fun foo() = "non-open member function"
    val bar = "non-open member property"
    fun nux() = "non-open member function"
    val zip = "non-open member property"
    open fun ril() = "open member function"
    open val det = "open member property"
}

