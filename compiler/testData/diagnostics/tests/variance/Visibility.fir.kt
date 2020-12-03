interface Test<in I, out O> {
    val internal_val: I
    public val public_val: I
    protected val protected_val: I
    <!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> val private_val: I

    var interlan_private_set: O
        private set
    public var public_private_set: O
        private set
    protected var protected_private_set: O
        private set
    <!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> var private_private_set: O
        private set

    fun internal_fun(i: O) : I
    public fun public_fun(i: O) : I
    protected fun protected_fun(i: O) : I
    private fun private_fun(i: O) : I
}