// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// LAMBDAS: INDY
// WITH_STDLIB
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: inlineFunWithPrivateMethod.kt

private inline fun g(x: String) = println(x)

fun call(c: Consumer<String>) = c.accept("")

fun box(): String {
    val obj = { call(::g) } // `g` is inaccessible in this scope
    obj()
    return "OK"
}

// FILE: Consumer.java

public interface Consumer<T> {
    void accept(T t);
}
