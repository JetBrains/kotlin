// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -Coroutines

class Controller {
    <!UNSUPPORTED_FEATURE!>suspend<!> fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    <!UNSUPPORTED_FEATURE!>operator<!> fun handleResult(x: String, y: Continuation<Nothing>) {}
}

fun builder(<!UNSUPPORTED_FEATURE!>coroutine<!> c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    <!UNSUPPORTED_FEATURE!>builder<!> {
        suspendHere()
    }

    return result
}
