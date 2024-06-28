// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: anonymousObjects.kt

package test

import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

interface I

fun check(x: KClass<*>, value: Any) {
    assertEquals(null, x.qualifiedName)

    assertEquals(
        setOf(
            "foo", "bar",
            "equals", "hashCode", "toString"
        ),
        x.members.mapTo(hashSetOf()) { it.name }
    )

    assertEquals(emptyList(), x.annotations)
    assertEquals(emptyList(), x.nestedClasses)
    assertEquals(null, x.objectInstance)
    assertEquals(emptyList(), x.sealedSubclasses)

    assertEquals(listOf(typeOf<I>(), typeOf<Any>()), x.supertypes)

    // Local class visibility cannot be represented in Kotlin, so `KClass.visibility` is null.
    assertEquals(null, x.visibility)

    // It's not really important whether the class is considered final or open, but it shouldn't be both (or neither).
    assertTrue(x.isFinal xor x.isOpen)

    assertFalse(x.isAbstract)
    assertFalse(x.isSealed)
    assertFalse(x.isData)
    assertFalse(x.isInner)
    assertFalse(x.isCompanion)
    assertFalse(x.isFun)
    assertFalse(x.isValue)

    assertFalse(x.isInstance(42))
    assertTrue(x.isInstance(value))

    val equals = x.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(value, value))

    val foo = x.members.single { it.name == "foo" }.apply { isAccessible = true } as KFunction<String>
    assertEquals("OK", foo.call(value))

    val bar = x.members.single { it.name == "bar" }.apply { isAccessible = true } as KProperty1<Any, Int>
    assertEquals(42, bar.get(value))
}

fun checkKotlinAnonymousObject() {
    val anonymousObject = object : I {
        val bar: Int = 42
        fun foo(): String = "OK"
    }
    val klass = anonymousObject::class

    // isAnonymousClass/simpleName behavior is different for Kotlin anonymous classes in JDK 1.8 and 9+, see KT-23072.
    if (klass.java.isAnonymousClass) {
        assertEquals(null, klass.simpleName)
    } else {
        assertEquals("anonymousObject\$1", klass.simpleName)
    }

    assertEquals(emptyList(), klass.constructors)

    check(klass, anonymousObject)
}

fun checkKotlinLocalClass() {
    class Local : I {
        val bar: Int = 42
        fun foo(): String = "OK"
    }
    val instance = Local()
    val klass = instance::class

    // simpleName behavior is different for Kotlin anonymous classes in JDK 1.8 and 9+, see KT-23072.
    assertTrue(klass.simpleName!!.endsWith("Local"))

    assertEquals(listOf("fun `<init>`(): test.`AnonymousObjectsKt\$checkKotlinLocalClass\$Local`"), klass.constructors.map { it.toString() })

    check(klass, instance)
}

fun checkJavaAnonymousObject() {
    val anonymousObject = JavaClass.anonymousObject()
    val klass = anonymousObject::class

    assertEquals(null, klass.simpleName)

    assertEquals(listOf("fun `<init>`(): `JavaClass$1`"), klass.constructors.map { it.toString() })

    check(klass, anonymousObject)
}

fun checkJavaLocalClass() {
    val instance = JavaClass.localClassInstance()
    val klass = instance::class

    assertEquals("Local", klass.simpleName)

    assertEquals(listOf("fun `<init>`(): Local"), klass.constructors.map { it.toString() })

    check(klass, instance)
}

fun box(): String {
    checkKotlinAnonymousObject()
    checkKotlinLocalClass()

    checkJavaAnonymousObject()
    checkJavaLocalClass()

    return "OK"
}

// FILE: JavaClass.java

public class JavaClass {
    public static Object anonymousObject() {
        return new test.I() {
            int bar = 42;
            String foo() { return "OK"; }
        };
    }

    public static Object localClassInstance() {
        class Local implements test.I {
            int bar = 42;
            String foo() { return "OK"; }
        }

        return new Local();
    }
}
