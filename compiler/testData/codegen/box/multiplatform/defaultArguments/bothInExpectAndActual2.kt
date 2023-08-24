// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND_K2: ANY
// FIR status: outdated code (expect and actual in the same module)

// FILE: common.kt

expect interface I {
    fun test(source: String = "expect")
}

expect interface J : I

// FILE: platform.kt

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS") // Counterpart for @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
actual interface I {
    // This test should be updated once KT-22818 is fixed; default values are not allowed in the actual function
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    actual fun test(source: String = "actual")
}

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual interface J : I {
    override fun test(source: String) {
        if (source != "actual") throw AssertionError(source)
    }
}

class K : J

fun box(): String {
    K().test()
    return "OK"
}
