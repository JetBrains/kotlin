// "Create property 'address' as constructor parameter" "true"
data class Person(val name: String, val age: Int)

fun person(): Person = TODO()

fun main(args: Array<String>) {
    val (name, age, address) = <caret>person()
}