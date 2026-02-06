// KJS_WITH_FULL_RUNTIME
package foo

fun box(): String {
    var s = "abc"
    assertEquals("ABC", (String::uppercase)(s))

    return "OK"
}
