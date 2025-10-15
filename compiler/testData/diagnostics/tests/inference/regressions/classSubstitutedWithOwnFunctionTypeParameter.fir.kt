// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70389
class B<T> {
    fun <R : T> m(x: B<R>) {
        x.m<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Any<!>>(x)
    }
}

class Foo<A> {
    fun <B : A> m(x: Foo<B>?) {
        x?.m<B>(null)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall, typeConstraint, typeParameter */
