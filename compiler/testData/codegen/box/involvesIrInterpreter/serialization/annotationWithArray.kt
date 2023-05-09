// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithVararg(vararg val array: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithArray(val array: Array<String>)

@AnnotationWithVararg("Str" <!EVALUATED("String")!>+ "ing"<!>, <!EVALUATED("String2")!>"String2"<!>, <!EVALUATED("String3")!>"String${3}"<!>)
class A

@AnnotationWithArray(["Str" <!EVALUATED("String")!>+ "ing"<!>, <!EVALUATED("String2")!>"String2"<!>, <!EVALUATED("String3")!>"String${3}"<!>])
class B

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
