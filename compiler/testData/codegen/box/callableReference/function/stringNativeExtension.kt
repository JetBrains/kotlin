// IGNORE_BACKEND: JVM_IR
// ^^^ Overload resolution ambiguity between candidates:
//     fun String.uppercase(): String
//     fun String.uppercase(locale: Locale)
// KJS_WITH_FULL_RUNTIME
package foo
import kotlin.test.assertEquals

fun box(): String {
    var s = "abc"
    assertEquals("ABC", (String::uppercase)(s))

    return "OK"
}
