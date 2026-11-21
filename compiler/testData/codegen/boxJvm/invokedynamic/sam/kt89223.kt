// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// 2 java/lang/invoke/LambdaMetafactory
// 0 private final static box\$foo
// 0 private final static box\$greet

import java.util.function.Function

class C {
    fun foo() {}
    fun greet(prefix: String) = "$prefix!"
}

fun consume(runnable: Runnable) {
    runnable.run()
}

fun consume(greeter: Function<String, String>) {
    check(greeter.apply("hello") == "hello!")
}

fun box(): String {
    consume(C()::foo)
    consume(C()::greet)
    return "OK"
}
