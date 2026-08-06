// LANGUAGE: +EliminateSecondKindIncorporation
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// DUMP_INFERENCE_LOGS: MARKDOWN

interface Box<T> {
    var x: T
}

fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun <E1> mySetOf1(x: E1): Set<E1> = TODO()
fun <E2> mySetOf2(x: E2): Set<E2> = TODO()

fun testWithSemifixation() {
    buildBox {
        x = mySetOf1("1")
        x = mySetOf2("2")
        x.size
    }.x.iterator().next().<!UNRESOLVED_REFERENCE("length;  on receiver of type 'Any?'")!>length<!> // Should work?
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, typeParameter, typeWithExtension */
