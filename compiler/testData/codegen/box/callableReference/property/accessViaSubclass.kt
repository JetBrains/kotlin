// IGNORE_BACKEND: JVM_IR
abstract class Base {
    val result = "OK"
}

class Derived : Base()

fun box(): String {
    return (Base::result).get(Derived())
}
