// RUN_PIPELINE_TILL: FRONTEND

error object E1
error object E2

fun <T : Any?, E : KError> removeErrors(v: T | E): T = v!!
fun <T, E : KError> removeErrors2(v: T | E): T = v!!

fun <T, E : KError> removeE1(v: T | E | E1): T | E = null!!

fun <T: Any? | KError> materialize(): T = null!!

fun foo() {
    val tmp = removeErrors(materialize<Int | E1>())
    val tmp1: Int = removeErrors(materialize<Int | E1>())
    val tmp2: Int = removeErrors2(materialize<Int | E1>())

    val tmp11: Int = removeErrors(materialize<Int | KError>())
    val tmp12: Int = removeErrors2(materialize<Int | KError>())

    val tmp3: Int | E2 = removeE1(materialize<Int | E1 | E2>())
    val tmp4: Int | KError = removeE1(materialize<Int | KError>())
}
