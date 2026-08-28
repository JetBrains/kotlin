// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JBase.java
public class JBase<T extends JBase, F extends T> {
    public T field = null;
    public F method() { return null; }
}

// FILE: JChild.java
public class JChild extends JBase {}

// FILE: KtSubclass.kt
open class KtSubclass : JChild()

// FILE: JChildChild.java
public class JChildChild extends KtSubclass {
    public JBase method() { return null; }
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun check(expected: String, reference: KCallable<*>, klass: KClass<*>) {
    assertEquals(expected, reference.toString())
    val fromMembers = klass.members.single { it.name == reference.name }
    assertEquals(expected, fromMembers.toString())
    assertEquals(reference, fromMembers)
}

fun box(): String {
    check("var JBase<T, F>.field: T!", JBase<*, *>::field, JBase::class)
    check("fun JBase<T, F>.method(): F!", JBase<*, *>::method, JBase::class)

    check("var JChild.field: JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", JChild::field, JChild::class)
    check("fun JChild.method(): JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", JChild::method, JChild::class)

    check("var KtSubclass.field: JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", KtSubclass::field, KtSubclass::class)
    check("fun KtSubclass.method(): JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", KtSubclass::method, KtSubclass::class)

    check("var JChildChild.field: JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", JChildChild::field, JChildChild::class)
    check("fun JChildChild.method(): JBase<(raw) JBase<*, *>!, (raw) JBase<*, *>!>", JChildChild::method, JChildChild::class)

    return "OK"
}
