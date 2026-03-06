// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import java.util.UUID
import java.util.UUID.randomUUID

OPTIONAL_JVM_INLINE_ANNOTATION
value class IdOne(val id: UUID)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IdTwo(val id: UUID)

fun box(): String {
    val sameUUID = randomUUID()
    val one = IdOne(sameUUID)
    val two = IdTwo(sameUUID)

    if (one.equals(two)) return "Fail"

    return "OK"
}