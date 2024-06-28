// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithVararg(vararg val array: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithArray(val array: Array<String>)

@AnnotationWithVararg(<!EVALUATED("String")!>"Str" + "ing"<!>, <!EVALUATED("String2")!>"String2"<!>, <!EVALUATED("String3")!>"String${3}"<!>)
class A

@AnnotationWithArray([<!EVALUATED("String")!>"Str" + "ing"<!>, <!EVALUATED("String2")!>"String2"<!>, <!EVALUATED("String3")!>"String${3}"<!>])
class B

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
