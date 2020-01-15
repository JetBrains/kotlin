// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
import kotlin.test.*

inline class S(val string: String)

fun foo(s: S) = s

fun box(): String {
    val fooRef = ::foo

    assertEquals("abc", fooRef.invoke(S("abc")).string)
    assertEquals("foo", fooRef.name)

    return "OK"
}