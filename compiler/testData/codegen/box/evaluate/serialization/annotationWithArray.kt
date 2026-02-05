// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithVararg(vararg val array: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithArray(val array: Array<String>)

@AnnotationWithVararg("Str" + "ing", "String2", "String${3}")
class A

@AnnotationWithArray(["Str" + "ing", "String2", "String${3}"])
class B

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return "OK"
}
