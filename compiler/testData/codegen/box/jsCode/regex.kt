// TARGET_BACKEND: JS_IR, JS_IR_ES6

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.3.0
// KT-78742: Supported in 2.3.20-Beta1

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
