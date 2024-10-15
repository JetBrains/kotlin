// RUN_PIPELINE_TILL: BACKEND
fun foo(s: String?): Int {
    while (s==null) {
    }
    return s.length
}