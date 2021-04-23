// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

private val privateProperty = 11;
private fun privateFun() {}

internal val internalProperty = 11;
internal fun internalFun() {}

public inline fun test() {
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateProperty<!>
}

public inline fun test2() {
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalProperty<!>
}

internal inline fun testInternal() {
    privateFun()
    privateProperty
}

internal inline fun test2Internal() {
    internalFun()
    internalProperty
}