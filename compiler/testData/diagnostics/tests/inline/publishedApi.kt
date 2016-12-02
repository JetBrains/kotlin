// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE
inline fun call(a: A) {
    a.test()
    testTopLevel()
}

@PublishedApi
internal class A {
    @PublishedApi
    internal fun test() {
        publicFun()
        internalFun()
        privateFun()
    }

    @PublishedApi
    internal inline fun testInline() {
        publicFun()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()
    }
}


@PublishedApi
internal fun testTopLevel() {
    publicFun()
    internalFun()
}

@PublishedApi
inline internal fun testTopLevelInline() {
    publicFun()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()
}

fun publicFun() {}

internal fun internalFun() {}

private fun privateFun() {}
