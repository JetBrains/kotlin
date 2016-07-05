// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
@AllowSuspendExtensions
class Controller

suspend fun Controller.noParams(c: Continuation<Unit>) {

}
suspend fun Controller.yieldString(value: String, c: Continuation<Unit>) {
}

suspend fun <V> Controller.await(f: () -> V, machine: Continuation<V>) {
}

suspend fun <V> Controller.await(f: Int, machine: Continuation<V>) {
}

suspend fun Controller.severalParams(x: String, y: Int, machine: Continuation<Double>) {
}

// These two must be prohibited because String and Any are not properly annotated
<!INAPPLICABLE_MODIFIER!>suspend<!> fun String.wrongReceiver(y: Int, machine: Continuation<Double>) {
}

<!INAPPLICABLE_MODIFIER!>suspend<!> fun Any.anyReceiver(y: Int, machine: Continuation<Double>) {
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

        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>wrongReceiver<!>(1)

        with("") {
            wrongReceiver(2<!NO_VALUE_FOR_PARAMETER!>)<!>
        }

        // Though such calls are allowed declarations with not-annotated receiver should be prohibited
        anyReceiver(3)
    }
}
