// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class A(val a: String?)

context(A) fun f() {
    if (this@A.a == null) return
    <!DEBUG_INFO_SMARTCAST!>this@A.a<!>.length
}