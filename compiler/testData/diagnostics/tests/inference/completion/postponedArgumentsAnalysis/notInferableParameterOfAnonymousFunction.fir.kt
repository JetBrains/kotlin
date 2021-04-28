// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

fun <T> select(vararg x: T) = x[0]
fun <K> id(x: K) = x

fun main() {
    val x1 = select<Any?>(id { x, y -> }, { x: Int, y -> })
    val x2 = <!NEW_INFERENCE_ERROR!>select(id { x, y -> }, { x: Int, y -> })<!>

    val x3 = <!NEW_INFERENCE_ERROR!>select(id(fun (x, y) {}), fun (x: Int, y) {})<!>

    val x4 = select<Any?>((fun (x, y) {}), fun (x: Int, y) {})
    val x5 = select<Any?>(id(fun (x, y) {}), fun (x: Int, y) {})
    val x6 = id<Any?>(fun (x) {})

    select<Any?>(fun (x) {}, fun (x) {})
}
