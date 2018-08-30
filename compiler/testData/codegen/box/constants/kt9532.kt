// IGNORE_BACKEND: JVM_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: NATIVE

object A {
    const val a: String = "$"
    const val b = "1234$a"
    const val c = 10000

    val bNonConst = "1234$a"
    val bNullable: String? = "1234$a"
}

object B {
    const val a: String = "$"
    const val b = "1234$a"
    const val c = 10000

    val bNonConst = "1234$a"
    val bNullable: String? = "1234$a"
}

fun box(): String {
    if (A.a !== B.a) return "Fail 1: A.a !== B.a"

    if (A.b !== B.b) return "Fail 2: A.b !== B.b"

    if (A.c !== B.c) return "Fail 3: A.c !== B.c"

    if (A.bNonConst !== B.bNonConst) return "Fail 4: A.bNonConst !== B.bNonConst"
    if (A.bNullable !== B.bNullable) return "Fail 5: A.bNullable !== B.bNullable"

    return "OK"
}