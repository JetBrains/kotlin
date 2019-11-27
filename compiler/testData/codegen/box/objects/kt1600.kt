// IGNORE_BACKEND_FIR: JVM_IR
abstract class Foo<T> {
    fun hello(id: T) = "O$id"
}

class Bar: Foo<String>() {
}

fun box() = Bar().hello("K")
