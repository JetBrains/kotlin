// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val anon = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> () {}
    val anon2 = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T : I><!> () {}
    val anon3 = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> () where T : I {}
    val anon4 = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> () where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>U<!> : I {}
    val anon5 = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> (x: T): T {
        val y: T = x
        return y
    }
}

interface I
