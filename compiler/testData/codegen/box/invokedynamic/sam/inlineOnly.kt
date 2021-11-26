// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: inlineOnly.kt
fun call(c: Consumer<String>) = c.accept("")

fun box(): String {
    call(::println) // 'println' is @InlineOnly
    return "OK"
}

// FILE: Consumer.java

public interface Consumer<T> {
    void accept(T t);
}
