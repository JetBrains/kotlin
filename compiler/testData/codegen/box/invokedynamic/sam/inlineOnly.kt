// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory

// FILE: inlineOnly.kt

inline fun <reified T> f(x: T) =
    println("${T::class.simpleName}($x)")

private inline fun g(x: String) = println(x)

fun call(c: Consumer<String>) = c.accept("")

fun box(): String {
    call(::println) // `println` is `@InlineOnly`
    call(::f) // `f` has a reified type parameter and thus isn't callable directly
    val obj = { call(::g) } // `g` is inaccessible in this scope
    obj()
    return "OK"
}

// FILE: Consumer.java

public interface Consumer<T> {
    void accept(T t);
}
