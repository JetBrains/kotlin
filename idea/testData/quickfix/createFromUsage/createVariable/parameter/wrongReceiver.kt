// "Create parameter 'value'" "true"
// DISABLE-ERRORS
trait Tr {
    fun foo(value: String, b: String) = ""
    fun bar() = foo(<caret>value, b)
}