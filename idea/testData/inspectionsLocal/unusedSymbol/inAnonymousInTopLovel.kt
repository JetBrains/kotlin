// PROBLEM: none
fun main() {
    writer.sayHello()
}

private val writer = object {
    fun <caret>sayHello() {
    }
}