// ISSUE: KT-61909
// REASON: unexpected yellow code in K1 (see corresponding diagnostic test)

// JVM_ABI_K1_K2_DIFF: KT-61909

// IGNORE_LIGHT_ANALYSIS
// REASON: unexpected red code (false-positive NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER in testYield)

fun box(): String {
    testYield()
    return "OK"
}

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    build {
        variable = arg
    }
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

var <EFT> Buildee<EFT>.variable: EFT
    get() = UserKlass() as EFT
    set(value) {}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
