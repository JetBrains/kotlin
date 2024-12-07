// RUN_PIPELINE_TILL: BACKEND
// KT-9051: Allow smart cast for captured variables if they are not modified

fun foo(y: String?) {
    var x: String? = ""
    if (x != null) {
        y?.let { x != y }
        // x is not changed, smart cast is possible
        x.length
    }
}
