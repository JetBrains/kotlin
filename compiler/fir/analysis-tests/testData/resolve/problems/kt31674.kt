// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-31674

// KT-31674: Wrong upper bound for star projection with recursive upper bound
interface A<T> { val out: T }
interface B<T : A<T>> { val out: T }

fun f(x: B<*>) = x.out.out

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, interfaceDeclaration, nullableType, propertyDeclaration,
starProjection, typeConstraint, typeParameter */
