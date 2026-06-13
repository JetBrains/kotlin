// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-41383

// KT-41383: Impossible to use callable reference to extension function as default parameter value
fun Int.plusWithDefault(i: Int = 0) = this + i
fun plusRef(ref: (Int) -> Int = Int::plusWithDefault) = ref(5)

fun works() = plusRef(Int::plusWithDefault)

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, thisExpression */
