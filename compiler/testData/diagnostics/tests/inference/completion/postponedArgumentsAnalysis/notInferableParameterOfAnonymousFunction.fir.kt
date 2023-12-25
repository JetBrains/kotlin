// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// COMPARE_WITH_LIGHT_TREE

fun <T> select(vararg x: T) = x[0]
fun <K> id(x: K) = x

fun main() {
    val x1 = select<Any?>(id { x, y -> }, { x: Int, y -> })
    val x2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE{LT}!><!CANNOT_INFER_PARAMETER_TYPE{PSI}!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!> { x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> }<!>, { x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> }<!>)

    val x3 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE{LT}!><!CANNOT_INFER_PARAMETER_TYPE{PSI}!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(fun (x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {})<!>, fun (x: Int, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {}<!>)

    val x4 = select<Any?>((fun (x, y) {}), fun (x: Int, y) {})
    val x5 = select<Any?>(id(fun (<!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!>) {}), fun (x: Int, y) {})
    val x6 = id<Any?>(fun (x) {})

    select<Any?>(fun (x) {}, fun (x) {})
}
