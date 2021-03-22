package functionParameter

fun foo(x: Int, y: Int, z: Int) {
    //Breakpoint!
    5
}
fun main() {
    foo(100, 10, 1)
}

// EXPRESSION: x + y + z
// RESULT: 111: I
