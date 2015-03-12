import kotlin.reflect.jvm.kotlin

class A(param: String) {
    val int: Int get() = 42
    val string: String = param
    var anyVar: Any? = null

    val List<IntRange>.extensionToList: Unit get() {}

    fun notAProperty() {}
}

fun box(): String {
    val klass = javaClass<A>().kotlin

    val props = klass.getProperties()

    val names = props.map { it.name }.toSortedList()
    assert(names == listOf("anyVar", "int", "string")) { "Fail names: $props" }

    val stringProp = props.firstOrNull { it.name == "string" } ?: return "Fail, string not found: $props"
    return stringProp.get(A("OK")) as String
}
