// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

abstract class GenericBase<T> {
    abstract fun foo(x: T): T
}

inline class Str(val str: String)

class Derived : GenericBase<Str>() {
    override fun foo(x: Str): Str = x
}

fun box() = Derived().foo(Str("OK")).str