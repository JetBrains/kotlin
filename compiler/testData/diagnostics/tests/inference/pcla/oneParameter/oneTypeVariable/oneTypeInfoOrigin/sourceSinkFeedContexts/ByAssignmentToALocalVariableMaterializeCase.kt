// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

/* TESTS */

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    val arg: UserKlass = UserKlass()
    val buildee = build {
        var temp = arg
        temp = materialize()
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
