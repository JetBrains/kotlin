// FIR_IDENTICAL
// LANGUAGE: +ReadDeserializedContracts +UseReturnsEffect
// DIAGNOSTICS: -INVISIBLE_REFERENCE, -INVISIBLE_MEMBER, -DEBUG_INFO_SMARTCAST
// SKIP_TXT

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

fun testRequireAndDefiniteReturn(x: Any, b: Boolean) {
    if (b) {
        require(x is String)
    } else {
        return
    }
    x.length
}


fun testRequireWithFailingMessageAndDefiniteReturn(x: Any, b: Boolean) {
    if (b) {
        require(x is String) { "x is not String!" }
    } else {
        return
    }
    x.length
}
