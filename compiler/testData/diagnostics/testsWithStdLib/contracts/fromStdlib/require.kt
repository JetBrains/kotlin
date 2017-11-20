// !LANGUAGE: +ReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testRequireSmartcast(x: Any?) {
    require(x is String)
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun testRequireUnreachableCode() {
    require(false)
    println("Can't get here!")
}

fun testRequireWithMessage(x: Any?) {
    require(x is String) { "x is not String!" }
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun testRequireWithFailingMessage(x: Any?) {
    require(x is String) { throw kotlin.IllegalStateException("What a strange idea") }
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun tesRequireNotNullWithMessage(x: Int?) {
    requireNotNull(x) { "x is null!"}
    <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}