// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is returned as a last statement of the builder argument
fun testLastStatementReturn() {
    val buildee = build {
        materialize()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

// PTV is returned explicitly
fun testExplicitReturn() {
    val buildee = build {
        return@build materialize()
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
    instructions: Buildee<FT>.() -> UserKlass
): Buildee<FT> {
    return Buildee<FT>().apply { this.instructions() }
}

class UserKlass
