class Something {
    public val publicVal1 = object { override fun toString() = "!" }
    protected val protectedVal1 = object { override fun toString() = "!" }
    internal val internalVal1 = object { override fun toString() = "!" }
    private val privateVal1 = object { override fun toString() = "!" }

    public val publicVal2 = run { class A; A() }
    protected val protectedVal2 = run { class A; A() }
    internal val internalVal2 = run { class A; A() }
    private val privateVal2 = run { class A; A() }

    public fun publicFun1() = object { override fun toString() = "!" }
    protected fun protectedFun1() = object { override fun toString() = "!" }
    internal fun internalFun1() = object { override fun toString() = "!" }
    private fun privateFun1() = object { override fun toString() = "!" }

    public fun publicFun2() = run { class A; A() }
    protected fun protectedFun2() = run { class A; A() }
    internal fun internalFun2() = run { class A; A() }
    private fun privateFun2() = run { class A; A() }
}