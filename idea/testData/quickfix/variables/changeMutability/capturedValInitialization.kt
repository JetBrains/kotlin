// "Make variable mutable" "true"
fun exec(f: () -> Unit) = f()

fun foo() {
    val x: Int
    exec {
        <caret>x = 42
    }
}
