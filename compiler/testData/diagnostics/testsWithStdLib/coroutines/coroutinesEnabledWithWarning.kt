// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: warn:Coroutines -ReleaseCoroutines

<!EXPERIMENTAL_FEATURE_WARNING!>suspend<!> fun suspendHere(): String = "OK"

fun builder(c: <!EXPERIMENTAL_FEATURE_WARNING!>suspend<!> () -> Unit) {

}

fun box(): String {
    var result = ""

    <!EXPERIMENTAL_FEATURE_WARNING!>builder<!> {
        suspendHere()
    }

    return result
}
