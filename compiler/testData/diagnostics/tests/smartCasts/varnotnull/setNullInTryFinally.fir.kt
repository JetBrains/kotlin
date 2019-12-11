// !LANGUAGE: +SoundSmartCastsAfterTry

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
    s.<!UNRESOLVED_REFERENCE!>hashCode<!>()
}