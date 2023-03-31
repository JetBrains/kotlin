// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithDefault(val str: String = "Str" + "ing")

@AnnotationWithDefault()
class A

@AnnotationWithDefault("Other")
class B

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}