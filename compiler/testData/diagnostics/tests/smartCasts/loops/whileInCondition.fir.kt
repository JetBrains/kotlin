// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    while (s!!.length > 0) {
        s.length
    }
    return s.length
}