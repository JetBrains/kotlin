// RUN_PIPELINE_TILL: BACKEND
// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-61909
// REASON: unexpected yellow code in K1

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> {
        variable = arg
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(<!TYPE_MISMATCH, TYPE_MISMATCH!>buildee<!>)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

var <EFT> Buildee<EFT>.variable: EFT
    get() = null!!
    set(value) {}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
