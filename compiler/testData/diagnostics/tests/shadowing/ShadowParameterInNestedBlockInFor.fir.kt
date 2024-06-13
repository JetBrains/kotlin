// DIAGNOSTICS: +UNUSED_PARAMETER +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun f(i: Int) {
    for (j in 1..100) {
        {
            var i = 12
        }
    }
}
