// !LANGUAGE: -SoundSmartCastsAfterTry

fun foo() {
    var s: String?
    s = "Test"
    try {
        s = null
    } catch (ex: Exception) {}
    <!DEBUG_INFO_SMARTCAST!>s<!>.hashCode()
}