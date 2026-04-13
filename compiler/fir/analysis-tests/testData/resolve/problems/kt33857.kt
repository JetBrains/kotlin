// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-33857
// RENDER_DIAGNOSTICS_FULL_TEXT

// KT-33857: COMPONENT_FUNCTION_AMBIGUITY diagnostic for destructuring in for loop
class A {
    operator <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    operator <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    operator fun component2() = 1
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, y) in <!COMPONENT_FUNCTION_AMBIGUITY!>C()<!>) {

    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, forLoop, functionDeclaration, integerLiteral, localProperty,
operator, propertyDeclaration */
