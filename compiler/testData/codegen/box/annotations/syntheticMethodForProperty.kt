// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// FULL_JDK

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
            assertEquals("[@Anno(value=OK)]", method.declaredAnnotations.toList().toString())
            return
        }
    }
    if (expected) {
        fail("Synthetic method for annotated property expected, but not found")
    }
}

fun box(): String {
    check(Class.forName("A"))
    check(Class.forName("SyntheticMethodForPropertyKt"))
    check(Class.forName("T"), expected = false)
    check(Class.forName("T\$DefaultImpls"))
    return "OK"
}
