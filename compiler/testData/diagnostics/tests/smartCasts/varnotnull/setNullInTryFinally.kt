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
    <!DEBUG_INFO_SMARTCAST!>s<!>.hashCode()
}