// "Create parameter 'value'" "true"
// DISABLE-ERRORS
interface Tr {
    fun foo(value: String, b: String) = ""
    fun bar() = foo(<caret>value, b)
}