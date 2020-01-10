// !LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testRequireSmartcast(x: Any?) {
    require(x is String)
    x.length
}

fun testRequireUnreachableCode() {
    require(false)
    println("Can't get here!")
}

fun testRequireWithMessage(x: Any?) {
    require(x is String) { "x is not String!" }
    x.length
}

fun testRequireWithFailingMessage(x: Any?) {
    require(x is String) { throw kotlin.IllegalStateException("What a strange idea") }
    x.length
}

fun tesRequireNotNullWithMessage(x: Int?) {
    requireNotNull(x) { "x is null!"}
    x.inc()
}