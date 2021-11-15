// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: reifiedTypeParameter.kt
class OK

inline fun <reified T> f(x: T) =
    T::class.simpleName

fun call(c: Consumer<OK>) = c.accept(OK())

fun box(): String {
    return call(::f) // `f` has a reified type parameter and thus isn't callable directly
}

// FILE: Consumer.java

public interface Consumer<T> {
    String accept(T t);
}
