// LANGUAGE: +ContextParameters
// TARGET_BACKEND: WASM
// WITH_STDLIB
// FILE: contextParametersJSModule.kt

external interface ContextA { fun value(): String }
external interface ContextB { fun value(): String }

@JsModule("./contextParametersJSModule.mjs")
external object JS {
    fun getContextA(): ContextA
    fun getContextB(): ContextB

//    EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS
//    context(a: ContextA) fun callValue(): String
//    context(a: ContextA) fun callValueWithPrefix(prefix: String): String
//    context(a: ContextA, b: ContextB) fun combineValues(): String
}

context(a: ContextA)
fun resolveValue(): String = a.value()

fun useAContextBlock(ctxA: ContextA): String = context(ctxA) { resolveValue() }
fun useAWith(ctxA: ContextA): String = with(ctxA) { resolveValue() }

context(a: ContextA)
fun useANested(): String = resolveValue()


fun box(): String {
    val ctxA = JS.getContextA()

    val ok =
        useAContextBlock(ctxA) == "OK" &&
                useAWith(ctxA) == "OK" &&
                context(ctxA) { useANested() } == "OK"
//                && context(ctxA) { JS.callValue() } == "OK" &&
//                context(ctxA) { JS.callValueWithPrefix("->") } == "->OK" &&
//                run {
//                    val ctxB = JS.getContextB()
//                    context(ctxA, ctxB) { JS.combineValues() } == "OKB"
//                }

    return if (ok) "OK" else "FAIL"
}

// FILE: contextParametersJSModule.mjs
export function getContextA() {
    return { value() { return "OK"; } };
}

export function getContextB() {
    return { value() { return "B"; } };
}

export function callValue(a) {
    return a.value();
}

export function callValueWithPrefix(a, prefix) {
    return prefix + a.value();
}

export function combineValues(a, b) {
    return a.value() + b.value();
}