// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.*

open class A {
    open fun foo(x: Int, y: Int = 1) {}
}

class B : A() {
    override fun foo(x: Int, y: Int) {}
}

class C : A()


fun Int.extFun() {}

class Z {
    context(c: String)
    fun context() {}
}

fun box(): String {
    assertEquals(listOf(false, false, true), A::foo.parameters.map { it.isOptional })
    assertEquals(listOf(false, false, true), B::foo.parameters.map { it.isOptional })
    assertEquals(listOf(false, false, true), C::foo.parameters.map { it.isOptional })

    assertFalse(Int::extFun.parameters.single().isOptional)

    val context = Z::class.members.single { it.name == "context" }
    assertEquals(listOf(false, false), context.parameters.map { it.isOptional })

    return "OK"
}
