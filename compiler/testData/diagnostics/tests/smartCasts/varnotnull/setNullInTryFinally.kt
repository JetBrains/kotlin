// FIR_IDENTICAL
// LANGUAGE: +SoundSmartCastsAfterTry

fun bar() {}

fun foo() {
    var s: String?
    s = "Test"
    try {
        s = null
    }
    catch (ex: Exception) {}
    finally {
        bar()
    }
    s<!UNSAFE_CALL!>.<!>hashCode()
}