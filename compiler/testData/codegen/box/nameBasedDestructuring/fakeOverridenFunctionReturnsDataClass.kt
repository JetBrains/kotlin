// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

data class Person(val id: Int, val name: String)

open class A {
    fun person(name: String) = Person(42, name)
}

class B : A()

fun box(): String {
    val b = B()
    val (name, id) = b.person("O")
    (val fullId = id, var fullName = name) = b.person("")
    fullName += "K"
    return name + fullName
}