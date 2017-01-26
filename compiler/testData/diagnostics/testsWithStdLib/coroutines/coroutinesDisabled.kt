// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +ErrorOnCoroutines

<!EXPERIMENTAL_FEATURE_ERROR!>suspend<!> fun suspendHere(): String = "OK"

fun builder(c: <!EXPERIMENTAL_FEATURE_ERROR!>suspend<!> () -> Unit) {

}

fun box(): String {
    var result = ""

    <!EXPERIMENTAL_FEATURE_ERROR!>builder<!> {
        suspendHere()
    }

    return result
}
