// ISSUE: KT-83308, fixed in 2.4.0-Beta1
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3

fun interface SomeFun {
    override fun toString(): String
}

fun foo(o: Any) = o.toString()

fun box(): String {
    val o = SomeFun { "O" }
    val k = SomeFun { "K" }
    return o.toString() + foo(k)
}