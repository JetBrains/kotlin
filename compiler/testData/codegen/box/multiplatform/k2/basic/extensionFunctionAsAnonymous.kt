// LANGUAGE: +MultiPlatformProjects
// DISABLE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: Property accessor 'a.<get-a>' can not be called: No property accessor found for symbol 'test/a.<get-a>|<get-a>(){}[1]'

// MODULE: common
// FILE: common.kt

package test

expect val a: String.(String) -> String


// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual val a = fun String.(y: String): String { return this + y }

fun box(): String {
    return if (a("O", "K") == "O".a("K")) "OK" else "FAIL"
}


