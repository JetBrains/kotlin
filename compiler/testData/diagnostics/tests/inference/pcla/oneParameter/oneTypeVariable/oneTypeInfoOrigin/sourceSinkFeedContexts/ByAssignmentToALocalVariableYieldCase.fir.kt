// CHECK_TYPE_WITH_EXACT
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// ISSUE: KT-63816
// REASON: unexpected red code in K1

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = build {
        var temp = materialize()
        temp = arg
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun materialize(): CT = null!!
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
