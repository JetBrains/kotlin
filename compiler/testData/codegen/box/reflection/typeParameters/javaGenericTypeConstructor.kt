// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test.kt

import kotlin.reflect.KVariance
import kotlin.test.assertEquals

fun box(): String {
    val ctor = J::class.constructors.single()
    val ab = ctor.typeParameters
    assertEquals(2, ab.size, ab.toString())

    assertEquals("A", ab[0].name)
    assertEquals(KVariance.INVARIANT, ab[0].variance)
    assertEquals("B", ab[1].name)
    assertEquals(KVariance.INVARIANT, ab[1].variance)

    // TODO: currently fails with "AssertionError: Expected <A>, actual <A>"
    // assertEquals(ab[0], ctor.parameters[0].type.classifier)

    assertEquals(ab[1], ctor.parameters[1].type.classifier)

    return "OK"
}

// FILE: J.java

public class J<A> {
    public <B> J(A a, B b) {}
}
