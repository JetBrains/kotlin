class Generic<T>
class Klass<T> {
    var mutableProperty: Generic<T> = Generic()
}

fun test() {
    val mutableProperty = Klass<*>::<!MUTABLE_PROPERTY_WITH_CAPTURED_TYPE!>mutableProperty<!>
    mutableProperty.set(Klass<Int>(), Generic<String>())
}