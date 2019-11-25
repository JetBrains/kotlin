// !LANGUAGE: -ProhibitDataClassesOverridingCopy
// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val a: A = B(1)
    a.copy(1)
    a.component1()
    return "OK"
}

interface A {
    fun copy(x: Int): A
    fun component1(): Any
}

data class B(val x: Int) : A
