// LANGUAGE: +SoundSmartCastsAfterTry

fun foo() {
    var s: String?
    s = "Test"
    try {
        s = "Other"
    } catch (ex: Exception) {}
    // Problem: here we do not see that 's' is always not-null
    s<!UNSAFE_CALL!>.<!>hashCode()
}