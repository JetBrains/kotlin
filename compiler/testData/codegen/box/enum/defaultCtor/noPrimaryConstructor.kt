// WITH_STDLIB
// IGNORE_BACKEND_K2: JS_IR, NATIVE

enum class Test {
    A(0),
    B;

    val n: Int

    constructor(n: Int) { this.n = n }
    constructor() : this(0)
}

fun box(): String =
    if (Test.A.n == Test.B.n)
        "OK"
    else
        "Fail"