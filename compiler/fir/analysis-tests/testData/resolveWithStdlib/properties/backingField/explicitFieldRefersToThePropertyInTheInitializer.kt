// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80606
// FIR_IDENTICAL
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// ^"FirImplicitTypeRefImplWithoutSource cannot be cast to org.jetbrains.kotlin.fir.types.FirResolvedTypeRef"
//  when tyring to calculate the return type of the backing field in `FirDataFlowAnalyzer`.open class A(val s: Any)

open class A(val s: Any)

val foo: Any
    field = object : A(<!UNINITIALIZED_VARIABLE!>foo<!>) {}

val bar: Any
    field = object : A(<!UNINITIALIZED_VARIABLE!>baz<!>) {}

val baz: Any
    field = object : A(bar) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, primaryConstructor, propertyDeclaration */
