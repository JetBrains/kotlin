fun <T> run(f: () -> T): T = f()


class Something {
    public val publicVal1 = object { override fun toString() = "!" }
    protected val protectedVal1 = object { override fun toString() = "!" }
    internal val internalVal1 = object { override fun toString() = "!" }
    private val privateVal1 = object { override fun toString() = "!" }

    public val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>publicVal2<!> = run { class A; A() }
    protected val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>protectedVal2<!> = run { class A; A() }
    internal val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>internalVal2<!> = run { class A; A() }
    private val privateVal2 = run { class A; A() }

    public fun publicFun1() = object { override fun toString() = "!" }
    protected fun protectedFun1() = object { override fun toString() = "!" }
    internal fun internalFun1() = object { override fun toString() = "!" }
    private fun privateFun1() = object { override fun toString() = "!" }

    public fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>publicFun2<!>() = run { class A; A() }
    protected fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>protectedFun2<!>() = run { class A; A() }
    internal fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>internalFun2<!>() = run { class A; A() }
    private fun privateFun2() = run { class A; A() }
}