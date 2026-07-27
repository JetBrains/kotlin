// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JBase.java
public class JBase<T extends JBase, F extends T> {
    public T field = null;
    public F method() { return null; }
}

// FILE: JChild.java
public class JChild extends JBase {}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class KtSubclass : JChild()

fun check(expected: String, reference: KCallable<*>, klass: KClass<*>, expectedReference: String? = null) {
    assertEquals(expectedReference ?: expected, reference.toString())
    val fromMembers = klass.members.single { it.name == reference.name }
    assertEquals(expected, fromMembers.toString())
    assertEquals(reference, fromMembers)
}

fun box(): String {
    check("var JBase<T, F>.field: T!", JBase<*, *>::field, JBase::class)
    check("fun JBase<T, F>.method(): F!", JBase<*, *>::method, JBase::class)

    check("var JChild.field: JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", JChild::field, JChild::class)
    check(
        "fun JChild.method(): JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", JChild::method, JChild::class,
        expectedReference = "fun JChild.method(): F!".takeUnless {
            Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true
        },
    )

    check("var KtSubclass.field: JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", KtSubclass::field, KtSubclass::class)
    check("fun KtSubclass.method(): JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", KtSubclass::method, KtSubclass::class)

    return "OK"
}
