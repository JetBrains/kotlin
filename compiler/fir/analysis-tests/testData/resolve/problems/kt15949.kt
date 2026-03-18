// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-15949

// KT-15949: typeinference couldn't filter out non-applicable generic candidates
interface I<T>

fun <T> I<T>.m(m: T) {}
fun <T> I<T>.m(block: () -> Unit) {}

fun call(obj: I<Int>, s: Int) {
    obj.m(s)
    obj.m {} // resolution ambiguity but should be successful
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, nullableType, typeParameter */
