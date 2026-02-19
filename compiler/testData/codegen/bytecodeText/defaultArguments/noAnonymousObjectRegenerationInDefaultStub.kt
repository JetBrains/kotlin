// FILE: test.kt

inline fun <reified T> test(x: T, p: String = "OK"): T {
    return object { fun f() = p as T }.f()
}

fun box() : String {
    return test("Fail")
}

// The test and test$default methods use the *same* anonymous object
// 2 INVOKESPECIAL TestKt\$test\$1.<init> \(Ljava/lang/String;\)V

// The box method has to regenerate it to instantiate the reified type parameter.
// 1 INVOKESPECIAL TestKt\$box\$\$inlined\$test\$default\$1.<init> \(Ljava/lang/String;\)V
