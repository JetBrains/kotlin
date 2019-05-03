package asyncFunctions

suspend fun main() {
    val a = 5
    val b = 7
    none()
    foo()
}

suspend fun foo() {
    val x = "foo"
    none()
    bar()
}

suspend fun bar() {
    var y = "zoo"
    none()
    val z = "bar"
    //Breakpoint!
    val a = 5
}

suspend fun none() {}