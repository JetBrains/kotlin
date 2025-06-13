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
    create<Interface> <!TYPE_MISMATCH("Interface; Unit")!>{
        boxOwner.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>unpack<!>()
    }<!>
}

/* GENERATED_FIR_TAGS: checkNotNullCall, funWithExtensionReceiver, functionDeclaration, functionalType,
interfaceDeclaration, lambdaLiteral, nullableType, typeParameter */
