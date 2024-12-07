// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNCHECKED_CAST, -UNREACHABLE_CODE

// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-60880
/* ATTENTION:
 * this test monitors an unfixed compiler bug;
 * if the behavior of the test changes, please consult with the linked YT ticket
 * to check whether the described problem has been fixed by your changes;
 * if the issue isn't actually fixed but new behavior persists,
 * please add a comment about the behavior change to the ticket
 * (preferably accompanied by an analysis of the change's reasons)
 */

class Buildee<CT> {
    fun materialize(): CT = null as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun testMaterialize() {
    fun consume(arg: Nothing) {}
    val buildee = build {
        consume(materialize())
    }
    checkExactType<Buildee<Nothing>>(buildee)
}
