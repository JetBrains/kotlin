// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val buildee = build {
        yield(it)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<UserKlass>>(buildee)
}

/* REQUIRED DECLARATIONS */

class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.(UserKlass) -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply { this.instructions(UserKlass()) }
}

class UserKlass
