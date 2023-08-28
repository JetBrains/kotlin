// IGNORE_BACKEND_K2: ANY
// accessing uninitialized parameter is illegal in FIR
// LANGUAGE: -ProhibitIllegalValueParameterUsageInDefaultArguments

fun f(
    f1: () -> String = { f2() },
    f2: () -> String = { "FAIL" }
): String = f1()

fun box(): String {
    var result = "fail"
    try {
        f()
    } catch (e : Exception) {
        result = "OK"
    }
    return f(f2 = { "O" }) + f(f1 = { "K" })
}
