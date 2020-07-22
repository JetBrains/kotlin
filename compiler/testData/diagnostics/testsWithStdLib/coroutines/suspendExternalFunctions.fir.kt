// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// !WITH_NEW_INFERENCE
suspend fun noParams() {
}

suspend fun yieldString(value: String) {}

suspend fun <V> await(f: () -> V) = f()

suspend fun <V> await(f: Int): V = null!!

suspend fun severalParams(x: String, y: Int) = 1.0

suspend fun String.stringReceiver(y: Int) = 1.0

suspend fun Any.anyReceiver(y: Int) = 1.0

fun builder(c: suspend () -> Unit) {}

fun test() {
    builder {
        noParams()
        yieldString("abc") checkType { _<Unit>() }
        <!INAPPLICABLE_CANDIDATE!>yieldString<!>(1) checkType { _<Unit>() }

        await<String> { "123" } checkType { _<String>() }

        // Inference from lambda return type
        await { 123 } checkType { _<Int>() }

        // Inference from expected type
        checkSubtype<String>(await(567))

        await<Double>(123) checkType { _<Double>() }

        severalParams("", 89) checkType { _<Double>() }
        // TODO: should we allow somehow to call with passing continuation explicitly?
        <!INAPPLICABLE_CANDIDATE!>severalParams<!>("", 89, 6.9) checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Unit>() }

        "".stringReceiver(1)
        Any().anyReceiver(1)

        with("") {
            stringReceiver(2)
        }
    }
}
