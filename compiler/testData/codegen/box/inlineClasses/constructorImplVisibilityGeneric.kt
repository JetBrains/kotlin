// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +GenericInlineClassParameter

import java.lang.reflect.Modifier

@JvmInline
value class IC1<T: Int> public constructor(val i: T)

@JvmInline
value class IC11<T: Int> internal constructor(val i: T)

@JvmInline
value class IC2<T: Int> private constructor(val i: T)

@JvmInline
value class IC4<T: Int> protected constructor(val i: T)

fun box(): String {
    if (!Modifier.isPublic(IC1::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 1"
    if (!Modifier.isPublic(IC11::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 1"
    if (!Modifier.isPrivate(IC2::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 2"
    if (!Modifier.isProtected(IC4::class.java.declaredMethods.single { it.name == "constructor-impl" }.modifiers)) return "FAIL 4"
    return "OK"
}