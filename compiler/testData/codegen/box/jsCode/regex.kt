// TARGET_BACKEND: JS_IR, JS_IR_ES6
// FILE: lib.kt
package foo

inline fun assertRegex(regexFactory: () -> dynamic, expectedPattern: String, expectedFlags: String, testString: String) {
    val reg = regexFactory()
    assertEquals(expectedPattern, reg.source)
    assertEquals(expectedFlags, reg.flags)
    assertEquals(true, reg.test(testString))
}

// FILE: main.kt
package foo

fun box(): String {
    assertRegex({ js("/ab+c/u") }, "ab+c", "u", "abbbbc")
    assertRegex({ js("/^ +$/") }, "^ +$", "", "   ")
    assertRegex({ js("/\\u{1F600}/u") }, "\\u{1F600}", "u", "\uD83D\uDE00")

    return "OK"
}
