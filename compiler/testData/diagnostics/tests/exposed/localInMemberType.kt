class Something {
    public val publicVal1 = object { override fun toString() = "!" }
    protected val protectedVal1 = object { override fun toString() = "!" }
    internal val internalVal1 = object { override fun toString() = "!" }
    private val privateVal1 = object { override fun toString() = "!" }

    public val <!EXPOSED_PROPERTY_TYPE!>publicVal2<!> = run { class A; A() }
    protected val <!EXPOSED_PROPERTY_TYPE!>protectedVal2<!> = run { class A; A() }
    internal val <!EXPOSED_PROPERTY_TYPE!>internalVal2<!> = run { class A; A() }
    private val privateVal2 = run { class A; A() }

    public fun publicFun1() = object { override fun toString() = "!" }
    protected fun protectedFun1() = object { override fun toString() = "!" }
    internal fun internalFun1() = object { override fun toString() = "!" }
    private fun privateFun1() = object { override fun toString() = "!" }

    public fun <!EXPOSED_FUNCTION_RETURN_TYPE!>publicFun2<!>() = run { class A; A() }
    protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>protectedFun2<!>() = run { class A; A() }
    internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>internalFun2<!>() = run { class A; A() }
    private fun privateFun2() = run { class A; A() }
}