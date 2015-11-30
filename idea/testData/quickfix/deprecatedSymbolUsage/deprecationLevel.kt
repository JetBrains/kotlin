// "Replace with 'b()'" "true"
@Deprecated("b!", ReplaceWith("b()"), DeprecationLevel.ERROR)
fun a() {}

fun b() {}

fun usage() {
    <caret>a()
}