// WASM_FAILS_IN_MULTI_MODULE_MODE_WINDOWS
// ISSUE: KT-84107
fun box(): String {
    testExplicitReturn()
    return "OK"
}

/* TESTS */

// PTV is returned explicitly
fun testExplicitReturn() {
    build {
        return@build materialize()
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun materialize(): CT = Unit as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply { this.instructions() }
}
