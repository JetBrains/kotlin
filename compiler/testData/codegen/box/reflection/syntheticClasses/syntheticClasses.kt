// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_TARGET: 1.8
// FILE: syntheticClasses.kt

package test

import kotlin.reflect.*
import kotlin.test.*

fun check(x: KClass<*>) {
    assertEquals(setOf("equals", "hashCode", "toString"), x.members.mapTo(hashSetOf()) { it.name })

    assertEquals(emptyList(), x.annotations)
    assertEquals(emptyList(), x.constructors)
    assertEquals(emptyList(), x.nestedClasses)
    assertEquals(null, x.objectInstance)
    assertEquals(listOf(typeOf<Any>()), x.supertypes)
    assertEquals(emptyList(), x.sealedSubclasses)

    assertEquals(KVisibility.PUBLIC, x.visibility)
    assertTrue(x.isFinal)
    assertFalse(x.isOpen)
    assertFalse(x.isAbstract)
    assertFalse(x.isSealed)
    assertFalse(x.isData)
    assertFalse(x.isInner)
    assertFalse(x.isCompanion)
    assertFalse(x.isFun)
    assertFalse(x.isValue)

    assertFalse(x.isInstance(42))
}

fun checkFileClass() {
    val fileClass = Class.forName("test.SyntheticClassesKt").kotlin
    assertEquals("SyntheticClassesKt", fileClass.simpleName)
    assertEquals("test.SyntheticClassesKt", fileClass.qualifiedName)
    check(fileClass)
}

fun checkMultifileClass() {
    val klass = Class.forName("test.MultifileClass").kotlin
    assertEquals("MultifileClass", klass.simpleName)
    assertEquals("test.MultifileClass", klass.qualifiedName)
    check(klass)
}

fun checkMultifileClassPart() {
    val klass = Class.forName("test.MultifileClass__Multifile1Kt").kotlin
    assertEquals("MultifileClass__Multifile1Kt", klass.simpleName)
    assertEquals("test.MultifileClass__Multifile1Kt", klass.qualifiedName)
    check(klass)
}

fun checkKotlinLambda() {
    // Annotate with @JvmSerializableLambda to prevent the lambda from being generated via invokedynamic.
    val lambda = @JvmSerializableLambda {}
    val klass = lambda::class

    // isAnonymousClass/simpleName behavior is different for Kotlin anonymous classes in JDK 1.8 and 9+, see KT-23072.
    if (klass.java.isAnonymousClass) {
        assertEquals(null, klass.simpleName)
    } else {
        assertEquals("lambda\$1", klass.simpleName)
    }

    assertEquals(null, klass.qualifiedName)
    check(klass)

    assertTrue(klass.isInstance(lambda))
    assertNotEquals(klass, (@JvmSerializableLambda {})::class)
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(lambda, lambda))
}

fun checkJavaLambda() {
    val lambda = JavaClass.lambda()
    val klass = lambda::class
    check(klass)

    assertTrue(klass.isInstance(lambda))
    assertNotEquals(klass, Runnable {}::class)
    val equals = klass.members.single { it.name == "equals" } as KFunction<Boolean>
    assertTrue(equals.call(lambda, lambda))
}

fun box(): String {
    checkFileClass()
    checkMultifileClass()
    checkMultifileClassPart()
    checkKotlinLambda()
    checkJavaLambda()

    return "OK"
}

// FILE: JavaClass.java

public class JavaClass {
    public static Runnable lambda() {
        return () -> {};
    }
}

// FILE: Multifile1.kt

@file:JvmName("MultifileClass")
@file:JvmMultifileClass

package test

fun f() {}

// FILE: Multifile2.kt

@file:JvmName("MultifileClass")
@file:JvmMultifileClass

package test

fun g() {}
