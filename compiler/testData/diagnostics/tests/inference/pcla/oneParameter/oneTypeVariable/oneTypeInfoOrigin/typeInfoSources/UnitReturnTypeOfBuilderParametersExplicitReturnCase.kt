// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is returned explicitly
fun testExplicitReturn() {
    val buildee = build {
        return@build materialize()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Unit>>(buildee)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun materialize(): CT = null!!
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply { this.instructions() }
}
