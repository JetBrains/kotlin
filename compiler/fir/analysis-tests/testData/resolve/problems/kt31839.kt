// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-31839

// KT-31839: Star projections erase too many type variables in the upper bound
interface B<T, U> { val outB: U }
interface A<T : B<T, U>, U> { val outA: T }
fun test(x: A<*, Int>): Int = x.outA.outB

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, interfaceDeclaration, nullableType, propertyDeclaration,
starProjection, typeConstraint, typeParameter */
