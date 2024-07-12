// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-61907
// REASON: unexpected red code in K1

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        variable = <!TYPE_MISMATCH!>arg<!>
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>buildee<!>)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    var variable: CT = null!!
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
