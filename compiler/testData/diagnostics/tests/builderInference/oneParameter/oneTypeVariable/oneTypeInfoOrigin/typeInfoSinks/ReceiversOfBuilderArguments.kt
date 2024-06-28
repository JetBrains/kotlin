// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: Buildee<UserKlass>) {}
    val buildee = build {
        consume(this)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT>

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass
