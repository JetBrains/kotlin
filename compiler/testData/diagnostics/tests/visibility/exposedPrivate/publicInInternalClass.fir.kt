// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C

private inline fun privateFun() = C()

internal open class A {
    public inline fun test() {
        <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>privateFun<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline */
