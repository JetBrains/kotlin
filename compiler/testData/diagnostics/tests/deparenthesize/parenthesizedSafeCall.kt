// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun f(s : String?) : Boolean {
    return (s?.equals("a"))!!
}