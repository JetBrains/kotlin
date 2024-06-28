// FIR_IDENTICAL
package test

annotation class AnnoA
annotation class AnnoB

enum class AnnotatedParameterInEnumConstructor(@AnnoA a: String, @AnnoB b: String) {
    A("1", "b")
}
