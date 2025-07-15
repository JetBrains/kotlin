// ISSUE: KT-52230
// IGNORE_BACKEND: JS_IR
// ^ Fixed in ES6 mode

// Prevent constant folding
fun id(x: Long) = x
fun add(a: Long, b: Long) = a + b

fun box(): String {
    if (id(0L) !== id(0L)) return "Fail 0L"
    if (id(add(123456789L, 1L)) !== id(123456790L)) return "Fail 0L"
    if (id(Long.MAX_VALUE) != id(Long.MAX_VALUE)) return "Fail Long.MAX_VALUE"
    return "OK"
}
