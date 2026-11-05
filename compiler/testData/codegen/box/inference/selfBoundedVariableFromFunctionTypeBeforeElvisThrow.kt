// WITH_STDLIB
// ISSUE: KT-91112

inline fun <reified T : Enum<T>, V> ((T) -> V).find(value: V): T? {
    return enumValues<T>().firstOrNull { this(it) == value }
}

enum class Animal(val string: String) {
    Cat("cat"),
    Dog("dog");

    companion object {
        fun fromString(string: String) = Animal::string.find(string)
            ?: throw IllegalArgumentException("Bad animal: '$string'")
    }
}

fun box(): String {
    if (Animal.fromString("dog") != Animal.Dog) return "Fail: dog"
    try {
        Animal.fromString("cow")
        return "Fail: no exception"
    } catch (e: IllegalArgumentException) {
        if (e.message != "Bad animal: 'cow'") return "Fail: ${e.message}"
    }
    return "OK"
}
