import kotlin.reflect.KProperty1

fun <T, R> getProperty(x: T, property: KProperty1<T, R>): R =
        property.get(x)

class Person(val name: String)

val name1 = getProperty(Person("John Smith"), Person::name)