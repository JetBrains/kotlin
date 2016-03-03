// "Change to property access" "false"
// ERROR: Expression 'fd' of type 'kotlin.String' cannot be invoked as a function. The function invoke() is not found
class A(val fd: String)

fun x() {
    val y = A("").f<caret>d("")
}
