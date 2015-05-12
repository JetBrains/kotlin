abstract class Foo<T> {
    fun hello(id: T) = "Hi $id"
}

interface Tr {
    fun hello(s : String)
}

class Bar: Foo<String>(), Tr {
}

fun box(): String = if (Bar().hello("Reg") == "Hi Reg") "OK" else "Fail"
