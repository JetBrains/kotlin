// !LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testCheckSmartcast(x: Any?) {
    check(x is String)
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun testCheckUnreachableCode() {
    check(false)
    // Can't be reported without notion of 'iff'
    println("Can't get here!")
}

fun testCheckWithMessage(x: Any?) {
    check(x is String) { "x is not String!" }
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun testCheckWithFailingMessage(x: Any?) {
    check(x is String) { throw kotlin.IllegalStateException("What a strange idea") }
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun testCheckNotNullWithMessage(x: Int?) {
    checkNotNull(x) { "x is null!" }
    <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}

fun testCheckNotNull(x: Int?) {
    checkNotNull(x)
    <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}