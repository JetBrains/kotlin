// RUN_PIPELINE_TILL: FRONTEND

error object E1
error object E2

fun <T : Any? | KError> materialize(): T = null!!

val tmp0: Int = materialize<Int?>()!!
val tmp1: Int = materialize<Int | E1>()!!
val tmp2: Int = materialize<Int | E1 | E2>()!!
val tmp3: Int = materialize<Int? | E1 | E2>()!!

fun <T : Any? | KError> foo(v: T) {
    val tmp: T & Any = v!!
}

fun <T : Any, E: KError> foo(v: T | E) {
    val tmp: T = v!!
}
