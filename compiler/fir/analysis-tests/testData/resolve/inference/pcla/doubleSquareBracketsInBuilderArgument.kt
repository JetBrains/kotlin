// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47982

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>build<!> {
        <!UNSUPPORTED!>[<!UNSUPPORTED!>[]<!>]<!>
    }
}




class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
