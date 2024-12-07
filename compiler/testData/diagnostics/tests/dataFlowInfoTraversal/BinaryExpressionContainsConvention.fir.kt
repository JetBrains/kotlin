// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(x: Number): Boolean {
    val result = (x as Int) in 1..5
    checkSubtype<Int>(x)
    return result
}
