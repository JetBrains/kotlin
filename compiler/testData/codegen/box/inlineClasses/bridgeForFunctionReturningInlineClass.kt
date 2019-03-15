// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class IC(val x: String)

interface I<T> {
    fun foo(): T
}

interface II: I<IC>

class A : I<IC> {
    override fun foo() = IC("O")
}

class B : II {
    override fun foo() = IC("K")
}

fun box() = A().foo().x + B().foo().x
