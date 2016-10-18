// "Rename to _" "true"

data class A(val x: String, val y: Int)

fun bar() {
    val (x<caret>, y) = A("", 1)
    y.hashCode()
}
