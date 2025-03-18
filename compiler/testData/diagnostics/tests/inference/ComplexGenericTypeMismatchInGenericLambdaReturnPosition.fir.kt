// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73051
// RENDER_DIAGNOSTICS_FULL_TEXT

interface Owner<O>
interface Box<B>
interface Interface

fun <C> create(lambda: () -> C): C = null!!

interface Pack<P>
fun <U> Owner<Pack<U>>.unpack(): U = null!!

fun test(boxOwner: Owner<Box<Interface>>) {
    create<Interface> {
        <!ARGUMENT_TYPE_MISMATCH!>boxOwner<!>.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>unpack<!>()
    }
}
