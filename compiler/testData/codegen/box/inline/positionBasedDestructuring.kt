// LANGUAGE: +NameBasedDestructuring

data class Person(val id: Int, val name: String)

inline fun person(name: String) = Person(42, name)

fun box(): String {
    var [id, name] = person("O")
    name += "K"
    return name
}