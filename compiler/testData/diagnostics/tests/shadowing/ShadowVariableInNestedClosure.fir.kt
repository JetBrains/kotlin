// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun f(): Int {
    var i = 17
    { var i = 18 }
    return i
}
