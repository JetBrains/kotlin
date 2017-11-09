class WithPublicInvoke {
    public operator fun invoke() {}
}

class WithInternalInvoke {
    internal operator fun invoke() {}
}

class WithProtectedInvoke {
    protected operator fun invoke() {}
}

class WithPrivateInvoke {
    private operator fun invoke() {}
}

class Test {
    public fun publicFoo() {}
    internal fun internalFoo() {}
    protected fun protectedFoo() {}
    private fun privateFoo() {}

    public val publicVal = 42
    internal val internalVal = 42
    protected val protectedVal = 42
    private val privateVal = 42

    public val withPublicInvoke = WithPublicInvoke()
    public val withInternalInvoke = WithInternalInvoke()
    public val withProtectedInvoke = WithProtectedInvoke()
    public val withPrivateInvoke = WithPrivateInvoke()
}

private fun Test.<!EXTENSION_SHADOWED_BY_MEMBER!>publicFoo<!>() {}
fun Test.internalFoo() {}
fun Test.protectedFoo() {}
fun Test.privateFoo() {}

val Test.<!EXTENSION_SHADOWED_BY_MEMBER!>publicVal<!>: Int get() = 42
val Test.internalVal: Int get() = 42
val Test.protectedVal: Int get() = 42
val Test.privateVal: Int get() = 42

fun Test.<!EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE!>withPublicInvoke<!>() {}
fun Test.wihtInternalInvoke() {}
fun Test.withProtectedInvoke() {}
fun Test.withPrivateInvoke() {}