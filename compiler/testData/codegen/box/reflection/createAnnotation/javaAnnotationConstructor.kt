// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J1.java
public @interface J1 {
    int value();
}

// FILE: J2.java
public @interface J2 {
    String value();
}

// FILE: JPackagePrivate.java
@interface JPackagePrivate {}

// FILE: box.kt
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf
import kotlin.reflect.KVisibility
import kotlin.reflect.KFunction
import kotlin.test.*

fun box(): String {
    val j1 = J1::class.primaryConstructor!!
    assertEquals("fun `<init>`(kotlin.Int): J1", j1.toString())
    assertEquals("<init>", j1.name)
    assertEquals(KVisibility.PUBLIC, j1.visibility)

    assertTrue(j1.isFinal)
    assertFalse(j1.isOpen)
    assertFalse(j1.isAbstract)
    assertFalse(j1.isSuspend)
    assertFalse(j1.isInline)
    assertFalse(j1.isExternal)
    assertFalse(j1.isOperator)
    assertFalse(j1.isInfix)

    assertEquals(typeOf<J1>(), j1.returnType)
    assertEquals("parameter #0 value of fun `<init>`(kotlin.Int): J1", j1.parameters.joinToString())
    assertEquals(emptyList(), j1.typeParameters)
    assertEquals(emptyList(), j1.annotations)

    val jPackagePrivate = JPackagePrivate::class.primaryConstructor!!
    assertEquals(null, jPackagePrivate.visibility)

    val j2 = J2::class.primaryConstructor!!
    assertEquals("fun `<init>`(kotlin.String): J2", j2.toString())
    assertNotEquals<KFunction<*>>(j1, j2)
    assertNotEquals<KFunction<*>>(j2, j1)

    return "OK"
}
