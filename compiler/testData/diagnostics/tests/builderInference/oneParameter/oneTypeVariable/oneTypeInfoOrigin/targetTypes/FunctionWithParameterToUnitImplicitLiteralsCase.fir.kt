// DIAGNOSTICS: -UNCHECKED_CAST

// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-60719
/* ATTENTION:
 * this test is supposed to monitor unclear feature behavior;
 * an explicit design decision regarding said behavior has not been made at the moment of test creation;
 * if the behavior of the test changes, please consult with the linked YT ticket
 * and either add a comment about the change if an explicit design decision is still unavailable
 * (preferably accompanied by an analysis of the change's reasons)
 * or remove this disclaimer otherwise
 */

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = { _: UserKlass -> } as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val buildee = build {
        yield { val x: UserKlass = <!UNRESOLVED_REFERENCE!>it<!> }
    }
    checkExactType<Buildee<(UserKlass) -> Unit>>(<!ARGUMENT_TYPE_MISMATCH("Buildee<kotlin/Function1<UserKlass, kotlin/Unit>>; Buildee<kotlin/Function0<kotlin/Unit>>")!>buildee<!>)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun <T> shareTypeInfo(from: T, to: T) {}
    val buildee = build {
        shareTypeInfo(
            { val x: UserKlass = <!UNRESOLVED_REFERENCE!>it<!> },
            materialize()
        )
    }
    checkExactType<Buildee<(UserKlass) -> Unit>>(<!ARGUMENT_TYPE_MISMATCH("Buildee<kotlin/Function1<UserKlass, kotlin/Unit>>; Buildee<kotlin/Function0<kotlin/Unit>>")!>buildee<!>)
}
