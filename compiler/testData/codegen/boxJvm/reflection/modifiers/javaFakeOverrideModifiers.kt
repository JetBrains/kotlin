// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: JBase.java

public class JBase {
    public String plus(int x) { return "JBase.plus" + x; }
    public String get(int i) { return "JBase.get" + i; }
    public String invoke() { return "JBase.invoke"; }
    public String notAnOperator(int x) { return "JBase.notAnOperator" + x; }
}

// FILE: JFakeOverrides.java

public class JFakeOverrides extends JBase {}

// FILE: box.kt

// Fake overrides of Java methods in Java classes keep the modifiers of the inherited method. For Java methods, `isOperator` is
// computed from the method's name and signature. See fakeOverrideModifiers.kt for Kotlin declarations, operatorsJava.kt for
// non-inherited Java methods, and callableModifiersInMixedHierarchies.kt for mixed hierarchies.

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun KClass<*>.function(name: String): KFunction<*> = members.single { it.name == name } as KFunction<*>

fun box(): String {
    val instance = JFakeOverrides()

    JFakeOverrides::class.function("plus").let {
        assertTrue(it.isOperator)
        assertEquals("JBase.plus1", it.call(instance, 1))
    }
    JFakeOverrides::class.function("get").let {
        assertTrue(it.isOperator)
        assertEquals("JBase.get2", it.call(instance, 2))
    }
    JFakeOverrides::class.function("invoke").let {
        assertTrue(it.isOperator)
        assertEquals("JBase.invoke", it.call(instance))
    }
    JFakeOverrides::class.function("notAnOperator").let {
        assertFalse(it.isOperator)
        assertFalse(it.isInfix)
        assertFalse(it.isInline)
        assertFalse(it.isSuspend)
        assertEquals("JBase.notAnOperator3", it.call(instance, 3))
    }

    return "OK"
}
