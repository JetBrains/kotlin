// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-49751
// DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

// KT-49751: False negative USELESS_CAST with interfaces
fun foo(x: I) {
    if (x is K) {
        test(x as K) // this unnecessary cast is not reported (false negative)
    }
}

fun foo2(x: Any) {
    if (x is K) {
        test(x <!USELESS_CAST!>as K<!>) // [USELESS_CAST] No cast needed
    }
}

fun test(o: K) {}
interface I {}
interface K {}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, smartcast */
