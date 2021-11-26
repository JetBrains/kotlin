
// IGNORE_BACKEND: JS, JS_IR, WASM
// IGNORE_BACKEND: JS_IR_ES6
// FULL_JDK
// WITH_STDLIB
// WASM_MUTE_REASON: IGNORED_IN_JS

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Id(val id: String)

fun throws() {
    throw RuntimeException()
}

fun test(id: Id) {
    throws()
}

fun foo() {
    test(Id("id"))
}

fun box(): String {
    val stackTrace = try {
        foo()
        throw AssertionError()
    } catch (e: RuntimeException) {
        e.stackTrace
    }

    for (entry in stackTrace) {
        if (entry.methodName.startsWith("test")) {
            return "OK"
        }
    }

    throw AssertionError(stackTrace.asList().toString())
}