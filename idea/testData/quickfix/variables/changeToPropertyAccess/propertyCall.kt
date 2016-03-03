// "Change to property access" "true"
class A(val ff: String)

fun x() {
    val y = A("").f<caret>f()
}
