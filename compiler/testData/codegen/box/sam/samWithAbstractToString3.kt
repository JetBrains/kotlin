// ISSUE: KT-83308, fixed in 2.4.0-Beta1
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3

fun interface SomeFun {
    override fun toString(): String
}

fun foo(o: Any) = o.toString()

fun box(): String {
    val oLambda: () -> String = { "O" }
    val kLambda: () -> String = { "K" }
    val o = SomeFun(oLambda)
    val k = SomeFun(kLambda)
    return o.toString() + foo(k)
}