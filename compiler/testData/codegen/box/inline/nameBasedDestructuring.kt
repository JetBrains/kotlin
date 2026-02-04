// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

data class Person(val id: Int, val name: String)

inline fun person(name: String) = Person(42, name)

fun box(): String {
    val (name, id) = person("O")
    (val fullId = id, var fullName = name) = person("")
    fullName += "K"
    return name + fullName
}