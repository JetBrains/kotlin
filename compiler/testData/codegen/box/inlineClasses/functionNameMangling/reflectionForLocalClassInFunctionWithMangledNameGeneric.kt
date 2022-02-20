// TARGET_BACKEND: JVM
// WITH_REFLECT
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

fun test(s: S<String>) {
    class Local

    val localKClass = Local::class
    val localJClass = localKClass.java

    val kName = localKClass.simpleName
    // See https://youtrack.jetbrains.com/issue/KT-29413
    // assertEquals("Local", kName)
    if (kName != "Local" && kName != "test\$Local") throw AssertionError("Fail KClass: $kName")

    assertTrue { localJClass.isLocalClass }

    val jName = localJClass.simpleName
    if (jName != "Local" && jName != "test\$Local") throw AssertionError("Fail java.lang.Class: $jName")
}

fun box(): String {
    test(S(""))

    return "OK"
}
