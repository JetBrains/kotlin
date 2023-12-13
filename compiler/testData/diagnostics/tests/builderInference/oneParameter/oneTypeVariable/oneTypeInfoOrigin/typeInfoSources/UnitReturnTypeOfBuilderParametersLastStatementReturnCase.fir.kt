// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-63477
/* ATTENTION:
 * this test is supposed to monitor unclear feature behavior;
 * an explicit design decision regarding said behavior has not been made at the moment of test creation;
 * if the behavior of the test changes, please consult with the linked YT ticket
 * and either add a comment about the change if an explicit design decision is still unavailable
 * (preferably accompanied by an analysis of the change's reasons)
 * or remove this disclaimer otherwise
 */

/* TESTS */

// PTV is returned as a last statement of the builder argument
fun testLastStatementReturn() {
    val buildee = build {
        materialize()
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
