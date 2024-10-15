// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +SoundSmartCastsAfterTry

fun foo() {
    var s: String?
    s = "Test"
    try {
        s = null
    } catch (ex: Exception) {}
    s<!UNSAFE_CALL!>.<!>hashCode()
}