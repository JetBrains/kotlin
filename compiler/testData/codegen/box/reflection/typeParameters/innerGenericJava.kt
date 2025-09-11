// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.java
public class A<T> {
    public class B<U> {
        public T test(U u) { return null; }
    }
}

// FILE: box.kt
import kotlin.reflect.KVariance
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

fun box(): String {
    val fn = A.B::class.members.single { it.name == "test" }

    val t = A::class.typeParameters.single()
    val u = A.B::class.typeParameters.single()

    assertEquals("T", t.name)
    assertEquals(KVariance.INVARIANT, t.variance)
    assertEquals(false, t.isReified)
    assertEquals("U", u.name)
    assertEquals(KVariance.INVARIANT, u.variance)
    assertEquals(false, u.isReified)

    assertNotEquals(t, u)

    assertEquals(t, fn.returnType.classifier)
    assertEquals(u, fn.parameters[1].type.classifier)

    return "OK"
}
