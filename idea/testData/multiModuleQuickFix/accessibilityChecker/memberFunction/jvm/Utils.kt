// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

interface Some

actual class A {
    actual fun <caret>a(): Some = TODO()
    private fun b() = "string"
    actual lateinit var c: Int
    const val d = "const"
}