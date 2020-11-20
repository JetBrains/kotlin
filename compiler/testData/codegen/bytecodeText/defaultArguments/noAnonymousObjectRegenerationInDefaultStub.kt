// FILE: test.kt

inline fun <reified T> test(x: T, p: String = "OK"): T {
    return object { fun f() = p as T }.f()
}

fun box() : String {
    return test("Fail")
}

// The test and test$default methods use the *same* anonymous object
// 2 INVOKESPECIAL TestKt\$test\$1.<init> \(Ljava/lang/String;\)V

// The box method has to regenerate it to instantiate the reified type parameter,
// but the name of the regenerated object differs between the JVM and JVM IR backends.
// JVM_TEMPLATES:
// 1 INVOKESPECIAL TestKt\$box\$\$inlined\$test\$1.<init> \(Ljava/lang/String;\)V
// JVM_IR_TEMPLATES:
// 1 INVOKESPECIAL TestKt\$box\$\$inlined\$test\$default\$1.<init> \(Ljava/lang/String;\)V
