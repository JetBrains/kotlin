// RUN_PIPELINE_TILL: BACKEND
fun f(s : String?) : Boolean {
    return foo@(s?.equals("a"))!!
}