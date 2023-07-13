// ISSUE: KT-60299

private typealias T = Unit

internal fun x(): T {
    val something = "OK"
    something.hashCode()
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
