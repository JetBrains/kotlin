// KT-3985

trait Trait<T> {
    fun f(): T
}

open class Class {
    fun f(): String = throw UnsupportedOperationException()
}

class Foo: Class(), Trait<String> {
}

fun box(): String {
    try {
        (Foo() : Trait<String>).f()
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
    return "Fail"
}
