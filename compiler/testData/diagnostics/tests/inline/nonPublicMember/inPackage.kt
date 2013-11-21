// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

private val privateProperty = 11;
private fun privateFun() {}

val internalProperty = 11;
fun internalFun() {}

public inline fun test() {
    <!INVISIBLE_MEMBER_FROM_INLINE!>privateFun<!>()
    <!INVISIBLE_MEMBER_FROM_INLINE!>privateProperty<!>
}

public inline fun test2() {
    <!INVISIBLE_MEMBER_FROM_INLINE!>internalFun<!>()
    <!INVISIBLE_MEMBER_FROM_INLINE!>internalProperty<!>
}

inline fun testInternal() {
    privateFun()
    privateProperty
}

inline fun test2Internal() {
    internalFun()
    internalProperty
}