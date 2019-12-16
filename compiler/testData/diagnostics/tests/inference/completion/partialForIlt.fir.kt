// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun takeByte(ilt: Byte) {}
fun takeShort(ilt: Short) {}
fun takeInt(ilt: Int) {}
fun takeLong(ilt: Long) {}

fun <T> id(arg: T): T = arg

fun test() {
    takeByte(id(42))
    takeShort(id(42))
    takeInt(id(42))
    takeLong(id(42))
}