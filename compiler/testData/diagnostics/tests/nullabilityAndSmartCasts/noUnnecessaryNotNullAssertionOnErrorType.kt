// RUN_PIPELINE_TILL: FRONTEND
package a

fun foo() {
    bar()!!
}

fun bar() = <!UNRESOLVED_REFERENCE!>aa<!>

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration */
