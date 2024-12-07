// test.AnnotatedParameterInEnumConstructor
// WITH_STDLIB
package test

annotation class Anno(val x: String)

enum class AnnotatedParameterInEnumConstructor(@Anno("a") a: String, @Anno("b") b: String) {
    A("1", "b")
}
