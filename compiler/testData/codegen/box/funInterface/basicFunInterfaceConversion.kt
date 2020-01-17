// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun box(): String {
    return foo { "OK" }
}