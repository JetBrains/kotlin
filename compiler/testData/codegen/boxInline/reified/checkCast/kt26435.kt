// WITH_STDLIB
// FILE: 1.kt
package test

enum class Id {
    OK,
    FAIL
}


sealed class Base(val id: Id)
class A(id: Id) : Base(id)
class B(id: Id) : Base(id)

inline fun <reified T : Base> process(t: T, f: (T) -> Unit): Base? {
    f(t)
    return getSomeBaseObject(t.id) as? T ?: throw RuntimeException()
}

fun getSomeBaseObject(id: Id): Base = if (id == Id.OK) A(id) else B(id)

// FILE: 2.kt
import test.*

fun doSth(base: Base): Base? =
    if (base is A) process(base, f = ::doSomethingInCaseOfA)
    else if (base is B) process(base, f = ::doSomethingInCaseOfB) else error("123")

fun doSomethingInCaseOfA(a: A) {}

fun doSomethingInCaseOfB(b: B) {}

fun box(): String {
    val a = doSth(A(Id.OK))!!

    return a.id.name
}
