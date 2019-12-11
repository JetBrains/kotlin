// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

private val privateProperty = 11;
private fun privateFun() {}

internal val internalProperty = 11;
internal fun internalFun() {}

public inline fun test() {
    privateFun()
    privateProperty
}

public inline fun test2() {
    internalFun()
    internalProperty
}

internal inline fun testInternal() {
    privateFun()
    privateProperty
}

internal inline fun test2Internal() {
    internalFun()
    internalProperty
}