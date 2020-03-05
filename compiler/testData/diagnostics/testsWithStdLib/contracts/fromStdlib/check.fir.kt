// !LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testCheckSmartcast(x: Any?) {
    check(x is String)
    x.length
}

fun testCheckUnreachableCode() {
    check(false)
    // Can't be reported without notion of 'iff'
    println("Can't get here!")
}

fun testCheckWithMessage(x: Any?) {
    check(x is String) { "x is not String!" }
    x.length
}

fun testCheckWithFailingMessage(x: Any?) {
    check(x is String) { throw kotlin.IllegalStateException("What a strange idea") }
    x.length
}

fun testCheckNotNullWithMessage(x: Int?) {
    checkNotNull(x) { "x is null!" }
    x.inc()
}

fun testCheckNotNull(x: Int?) {
    checkNotNull(x)
    x.inc()
}