// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-63816
// REASON: unexpected red code in K1 (see corresponding diagnostic test)
// WASM_FAILS_IN_MULTI_MODULE_MODE_WINDOWS
// ISSUE: KT-84107

fun box(): String {
    testYield()
    return "OK"
}

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    build {
        var temp = materialize()
        temp = arg
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
