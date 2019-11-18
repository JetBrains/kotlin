// IGNORE_BACKEND_FIR: JVM_IR
// KT-3985

interface Trait<T> {
    fun f(): T
}

open class Class {
    fun f(): String = throw UnsupportedOperationException()
}

class Foo: Class(), Trait<String> {
}

fun box(): String {
    val t: Trait<String> = Foo()
    try {
        t.f()
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
    return "Fail"
}
