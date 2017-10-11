package codegen.classDelegation.property

import kotlin.test.*

interface A {
    val x: Int
}

class C: A {
    override val x: Int = 42
}

class Q(a: A): A by a

fun box(): String {
    val q = Q(C())
    val a: A = q
    return q.x.toString() + a.x.toString()
}

@Test fun runTest() {
    println(box())
}