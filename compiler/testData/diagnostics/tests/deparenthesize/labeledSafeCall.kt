// RUN_PIPELINE_TILL: BACKEND
fun f(s : String?) : Boolean {
    return <!REDUNDANT_LABEL_WARNING!>foo@<!>(s?.equals("a"))!!
}