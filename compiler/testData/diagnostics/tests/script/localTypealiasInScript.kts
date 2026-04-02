// RUN_PIPELINE_TILL: FRONTEND

fun foo() {
    <!UNSUPPORTED_FEATURE!>typealias Local = String<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
