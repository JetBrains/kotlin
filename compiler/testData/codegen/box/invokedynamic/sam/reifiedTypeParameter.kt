// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory

// FILE: reifiedTypeParameter.kt

inline fun <reified T> f(x: T) =
    println("${T::class.simpleName}($x)")

fun call(c: Consumer<String>) = c.accept("")

fun box(): String {
    call(::f) // `f` has a reified type parameter and thus isn't callable directly
    return "OK"
}

// FILE: Consumer.java

public interface Consumer<T> {
    void accept(T t);
}
