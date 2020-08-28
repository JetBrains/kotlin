package asyncFunctions

suspend fun main() {
    val a = 5
    val b = 7
    none()
    foo()
    val dead1 = a
    val dead2 = b
}

suspend fun foo() {
    val x = "foo"
    none()
    bar()
    val dead = x
}

suspend fun bar() {
    var y = "zoo"
    none()
    val z = "bar"
    //Breakpoint!
    val a = 5
    val dead = y
}

suspend fun none() {}