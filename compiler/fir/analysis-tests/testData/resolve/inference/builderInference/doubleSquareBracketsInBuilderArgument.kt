// ISSUE: KT-47982

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, UNSUPPORTED!>[<!UNSUPPORTED!>[]<!>]<!>
    }
}




class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
