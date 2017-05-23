// ERROR: Cannot inline property with accessor(s) and backing field

val C = 239
    get() = field + 1

fun f() {
    println(<caret>C)
}