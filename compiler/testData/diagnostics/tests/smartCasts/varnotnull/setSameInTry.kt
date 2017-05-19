fun foo() {
    var s: String?
    s = "Test"
    try {
        s = "Other"
    } catch (ex: Exception) {}
    <!DEBUG_INFO_SMARTCAST!>s<!>.hashCode()
}