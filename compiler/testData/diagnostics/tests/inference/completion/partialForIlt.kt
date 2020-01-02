// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun takeByte(ilt: Byte) {}
fun takeShort(ilt: Short) {}
fun takeInt(ilt: Int) {}
fun takeLong(ilt: Long) {}

fun <T> id(arg: T): T = arg

fun test() {
    takeByte(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte")!>id(42)<!>)
    takeShort(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short")!>id(42)<!>)
    takeInt(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>id(42)<!>)
    takeLong(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>id(42)<!>)
}