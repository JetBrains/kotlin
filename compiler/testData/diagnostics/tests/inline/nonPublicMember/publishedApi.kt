// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE

// FIR_IDENTICAL


inline fun call(a: A) {
    a.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>test<!>()

    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
}

@PublishedApi
internal inline fun callFromPublishedApi(a: A) {
    a.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>test<!>()

    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
}

internal class A {
    @PublishedApi
    internal fun test() {
        test()

        privateFun()

        internalFun()
    }

    @PublishedApi
    internal fun testInline() {
        test()

        privateFun()

        internalFun()
    }
}


private fun privateFun() {

}

internal fun internalFun() {

}
