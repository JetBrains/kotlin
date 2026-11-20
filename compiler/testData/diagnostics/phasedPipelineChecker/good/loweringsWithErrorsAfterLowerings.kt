// RUN_PIPELINE_TILL: LOWERINGS

open class A {
    <!TAILREC_ON_VIRTUAL_MEMBER_ERROR!>tailrec<!> open fun foo(x: Int) {
        foo(x)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, tailrec */
