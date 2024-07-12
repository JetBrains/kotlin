// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

/* TESTS */

// PTV is in consuming position (yield-case)
fun testYield() {
    val buildee = build {
        it.yield(this)
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
    instructions: UserKlass.(Buildee<FT>) -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply { UserKlass().instructions(this) }
}

class UserKlass
