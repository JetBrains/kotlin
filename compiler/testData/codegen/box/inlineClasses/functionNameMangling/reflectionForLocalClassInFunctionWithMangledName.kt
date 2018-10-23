// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR, JS, JS_IR, NATIVE
// WITH_REFLECT
import kotlin.test.*

inline class S(val string: String)

fun test(s: S) {
    class Local

    val localKClass = Local::class
    val localJClass = localKClass.java

    assertEquals("test\$Local", localKClass.simpleName)

    assertTrue { localJClass.isLocalClass }
    assertEquals("test\$Local", localJClass.simpleName)
}

fun box(): String {
    test(S(""))

    return "OK"
}
