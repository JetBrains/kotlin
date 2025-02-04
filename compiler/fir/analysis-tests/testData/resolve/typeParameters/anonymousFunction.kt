// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val anon = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> () {}
    val anon2 = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T : I><!> () {}
    val anon3 = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> () where T : I {}
}

interface I