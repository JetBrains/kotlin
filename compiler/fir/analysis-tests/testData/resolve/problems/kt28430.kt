// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-28430

// KT-28430: Weird resolution for `Nothing?` + non-null assertion in new inference
class A {
    fun m1() = true
}

fun f1(x: A?) {
    if (x == null) {
        x<!UNSAFE_CALL!>.<!>m1() // unsafe call, x is {Nothing? & A?}
        x!!.m1() // unresolved reference (bug)
        x.m1() // OK, because x is {Nothing & A?}
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
nullableType, smartcast */
