// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
class Controller {
    suspend fun noParams(c: Continuation<Unit>) {

    }
    suspend fun yieldString(value: String, c: Continuation<Unit>) {
    }

    suspend fun <V> await(f: () -> V, machine: Continuation<V>) {
    }

    suspend fun <V> await(f: Int, machine: Continuation<V>) {
    }

    suspend fun severalParams(x: String, y: Int, machine: Continuation<Double>) {
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {}

fun test() {
    builder {
        noParams()
        yieldString("abc") checkType { _<Unit>() }
        yieldString(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>) checkType { _<Unit>() }

        await<String> { "123" } checkType { _<String>() }

        // Inference from lambda return type
        await { 123 } checkType { _<Int>() }

        // Inference from expected type
        checkSubtype<String>(await(567))

        await<Double>(123) checkType { _<Double>() }

        severalParams("", 89) checkType { _<Double>() }
        // TODO: prohibit such calls
        severalParams("", 89, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>6.9<!>) checkType { _<Unit>() }
        severalParams("", 89, this <!CAST_NEVER_SUCCEEDS!>as<!> Continuation<Double>) checkType { _<Unit>() }
    }
}
