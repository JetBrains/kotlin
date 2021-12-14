// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import java.lang.reflect.Modifier

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1 public constructor(val i: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC11 internal constructor(val i: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2 private constructor(val i: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC4 protected constructor(val i: Int)

fun box(): String {
    if (!Modifier.isPublic(IC1::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 1"
    if (!Modifier.isPublic(IC11::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 1"
    if (!Modifier.isPrivate(IC2::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 2"
    if (!Modifier.isProtected(IC4::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 4"
    return "OK"
}