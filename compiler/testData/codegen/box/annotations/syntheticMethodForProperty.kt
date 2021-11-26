// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package test

import java.lang.reflect.Modifier
import kotlin.test.*

annotation class Anno(val value: String)

class A {
    @Anno("OK") val property: Int
        get() = 42
}

interface T {
    @Anno("OK") val property: Int
}

@Anno("OK") val property: Int
    get() = 42

fun check(clazz: Class<*>, expected: Boolean = true) {
    for (method in clazz.getDeclaredMethods()) {
        if (method.getName() == "getProperty\$annotations") {
            if (!expected) {
                fail("Synthetic method for annotated property found, but not expected: $method")
            }
            assertTrue(method.isSynthetic())
            assertTrue(Modifier.isStatic(method.modifiers))
            assertTrue(Modifier.isPublic(method.modifiers))
            val str = method.declaredAnnotations.single().toString()
            assertTrue("@test.Anno\\((value=)?\"?OK\"?\\)".toRegex().matches(str), str)
            return
        }
    }
    if (expected) {
        fail("Synthetic method for annotated property expected, but not found")
    }
}

fun box(): String {
    check(Class.forName("test.A"))
    check(Class.forName("test.SyntheticMethodForPropertyKt"))
    check(Class.forName("test.T"), expected = false)
    check(Class.forName("test.T\$DefaultImpls"))
    return "OK"
}
