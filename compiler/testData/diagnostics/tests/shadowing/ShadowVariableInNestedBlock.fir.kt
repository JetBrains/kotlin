// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun ff(): Int {
    var i = 1
    {
        val i = 2
    }
    return i
}
