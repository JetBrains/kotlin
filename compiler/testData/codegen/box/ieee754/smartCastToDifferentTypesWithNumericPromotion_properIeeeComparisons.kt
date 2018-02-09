// !LANGUAGE: +ProperIeee754Comparisons
// IGNORE_BACKEND: JS

fun eqDI(x: Any?, y: Any?) = x is Double?   && y is Int?        && x == y
fun eqDL(x: Any?, y: Any?) = x is Double?   && y is Long?       && x == y
fun eqID(x: Any?, y: Any?) = x is Int?      && y is Double?     && x == y
fun eqLD(x: Any?, y: Any?) = x is Long?     && y is Double?     && x == y
fun eqFI(x: Any?, y: Any?) = x is Float?    && y is Int?        && x == y
fun eqFL(x: Any?, y: Any?) = x is Float?    && y is Long?       && x == y
fun eqIF(x: Any?, y: Any?) = x is Int?      && y is Float?      && x == y
fun eqLF(x: Any?, y: Any?) = x is Long?     && y is Float?      && x == y

fun testNullNull() {
    if (!eqDI(null, null)) throw Exception()
    if (!eqDL(null, null)) throw Exception()
    if (!eqID(null, null)) throw Exception()
    if (!eqLD(null, null)) throw Exception()
    if (!eqFI(null, null)) throw Exception()
    if (!eqFL(null, null)) throw Exception()
    if (!eqIF(null, null)) throw Exception()
    if (!eqLF(null, null)) throw Exception()
}

fun testNull0() {
    if (eqDI(null, 0)) throw Exception()
    if (eqDL(null, 0L)) throw Exception()
    if (eqID(null, 0.0)) throw Exception()
    if (eqLD(null, 0.0)) throw Exception()
    if (eqFI(null, 0)) throw Exception()
    if (eqFL(null, 0L)) throw Exception()
    if (eqIF(null, 0.0F)) throw Exception()
    if (eqLF(null, 0.0F)) throw Exception()
}

fun test0Null() {
    if (eqDI(0.0, null)) throw Exception()
    if (eqDL(0.0, null)) throw Exception()
    if (eqID(0, null)) throw Exception()
    if (eqLD(0L, null)) throw Exception()
    if (eqFI(0.0F, null)) throw Exception()
    if (eqFL(0.0F, null)) throw Exception()
    if (eqIF(0, null)) throw Exception()
    if (eqLF(0L, null)) throw Exception()
}

fun testPlusMinus0() {
    if (!eqDI(-0.0, 0)) throw Exception()
    if (!eqDL(-0.0, 0L)) throw Exception()
    if (!eqID(0, -0.0)) throw Exception()
    if (!eqLD(0L, -0.0)) throw Exception()
    if (!eqFI(-0.0F, 0)) throw Exception()
    if (!eqFL(-0.0F, 0L)) throw Exception()
    if (!eqIF(0, -0.0F)) throw Exception()
    if (!eqLF(0L, -0.0F)) throw Exception()
}

fun test01() {
    if (eqDI(0.0, 1)) throw Exception()
    if (eqDL(0.0, 1L)) throw Exception()
    if (eqID(0, 1.0)) throw Exception()
    if (eqLD(0L, 1.0)) throw Exception()
    if (eqFI(0.0F, 1)) throw Exception()
    if (eqFL(0.0F, 1L)) throw Exception()
    if (eqIF(0, 1.0F)) throw Exception()
    if (eqLF(0L, 1.0F)) throw Exception()
}

fun box(): String {
    testNullNull()
    testNull0()
    test0Null()
    testPlusMinus0()
    test01()
    
    return "OK"
}