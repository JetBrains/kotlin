data class Person(val name: String, val age: Int)

fun test() {
    val person = Person("", 0)
    val (name)<caret> = person
}
