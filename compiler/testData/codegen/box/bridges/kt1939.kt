// IGNORE_BACKEND_FIR: JVM_IR
abstract class Foo<T> {
    fun hello(id: T) = "Hi $id"
}

interface Tr {
    fun hello(s : String): String
}

class Bar: Foo<String>(), Tr {
}

fun box(): String = if (Bar().hello("Reg") == "Hi Reg") "OK" else "Fail"
