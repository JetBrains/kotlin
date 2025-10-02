// RUN_PIPELINE_TILL: FRONTEND

fun foo() {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Local = String<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
