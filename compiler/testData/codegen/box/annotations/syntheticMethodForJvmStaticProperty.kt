// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

package test

import java.lang.reflect.Modifier
import kotlin.test.*

class WithCompanionJvmStatic {

    companion object {
        @JvmStatic
        val property: Int
            get() = 42
    }
}

interface InterfaceWithCompanionJvmStatic {

    fun defaultImplsTrigger() = 123

    companion object {
        @JvmStatic
        val property: Int
            get() = 42
    }
}

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
            assertTrue("@kotlin.jvm.JvmStatic\\(\\)".toRegex().matches(str), str)
            return
        }
    }
    if (expected) {
        fail("Synthetic method for annotated property expected, but not found")
    }
}

fun box(): String {
    check(Class.forName("test.WithCompanionJvmStatic"), expected = false)
    check(Class.forName("test.WithCompanionJvmStatic\$Companion"))

    check(Class.forName("test.InterfaceWithCompanionJvmStatic"), expected = false)
    check(Class.forName("test.InterfaceWithCompanionJvmStatic\$DefaultImpls"), expected = false)
    check(Class.forName("test.InterfaceWithCompanionJvmStatic\$Companion"))

    return "OK"
}
