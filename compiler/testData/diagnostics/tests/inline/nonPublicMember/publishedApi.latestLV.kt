// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE

inline fun call(a: <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>A<!>) {
    a.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>test<!>()

    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
}

@PublishedApi
internal inline fun callFromPublishedApi(a: <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>A<!>) {
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
