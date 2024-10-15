// RUN_PIPELINE_TILL: SOURCE
fun foo(): Int {
    var s: String? = "abc"
    s = null
    return s<!UNSAFE_CALL!>.<!>length
}
