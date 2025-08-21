// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// IS_APPLICABLE: false
fun foo(s: String?) {
    val <!UNUSED_VARIABLE!>t<!>: String = s.toString()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration */
