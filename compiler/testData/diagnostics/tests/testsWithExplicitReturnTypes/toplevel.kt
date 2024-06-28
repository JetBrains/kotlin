// FIR_IDENTICAL
// SKIP_TXT

/**
 * foo KDoc
 */
fun foo() {}

public fun foo2() {}

fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>bar<!>() = 10
public fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>bar2<!>() = 10
public fun bar3(): Int = 10
