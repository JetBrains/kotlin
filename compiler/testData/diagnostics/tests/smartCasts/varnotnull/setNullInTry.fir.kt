// !LANGUAGE: +SoundSmartCastsAfterTry

fun foo() {
    var s: String?
    s = "Test"
    try {
        s = null
    } catch (ex: Exception) {}
    s.<!UNRESOLVED_REFERENCE!>hashCode<!>()
}