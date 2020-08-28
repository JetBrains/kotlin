data class Person(val name: String, val age: Int, val location: String)

fun test() {
    val (_, age)<caret> = Person("", 0, "")
}
