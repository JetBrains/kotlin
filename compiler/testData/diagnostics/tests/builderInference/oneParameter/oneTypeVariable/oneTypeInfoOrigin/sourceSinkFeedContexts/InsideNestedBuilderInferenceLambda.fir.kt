// DIAGNOSTICS: -UNCHECKED_CAST

// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-60663
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
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

class NestedBuildee<T> {
    fun nestedYield(arg: T) {}
}
fun <T> nestedBuild(
    instructions: NestedBuildee<T>.() -> Unit
): NestedBuildee<T> {
    return NestedBuildee<T>().apply(instructions)
}
class Placeholder

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        nestedBuild {
            yield(arg)
            nestedYield(Placeholder())
        }
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>checkExactType<!><<!CANNOT_INFER_PARAMETER_TYPE!>Buildee<UserKlass><!>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        nestedBuild {
            consume(materialize())
            nestedYield(Placeholder())
        }
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>checkExactType<!><<!CANNOT_INFER_PARAMETER_TYPE!>Buildee<UserKlass><!>>(buildee)
}
