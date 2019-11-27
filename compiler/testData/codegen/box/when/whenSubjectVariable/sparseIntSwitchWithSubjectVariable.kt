// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +VariableDeclarationInWhenSubject
// WITH_RUNTIME

fun sparse(x: Int): Int {
    return when (val xx = (x % 4) * 100) {
        100 -> 1
        200 -> xx / 100
        300 -> 3
        else -> 4
    }
}

fun box(): String {
    var result = (0..3).map(::sparse).joinToString()

    if (result != "4, 1, 2, 3") return "sparse:" + result
    return "OK"
}
