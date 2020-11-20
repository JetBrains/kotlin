// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun testFun1(str: String): String {
    val local = str

    class Local {
        fun foo() = str
    }

    val list = listOf(0).map { Local() }
    return list[0].foo()
}

fun box(): String {
    return when {
        testFun1("test1") != "test1" -> "Fail #1"
        else -> "OK"
    }
}
