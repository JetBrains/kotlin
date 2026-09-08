// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory
// 0 private final static box\$foo

class C {
    fun foo() {}
}

fun consume(runnable: Runnable) {
    runnable.run()
}

fun box(): String {
    consume(C()::foo)
    return "OK"
}
