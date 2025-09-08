// RUN_PIPELINE_TILL: FRONTEND
typealias TopLevelInScript = String

class C {
    <!UNSUPPORTED_FEATURE!>typealias NestedInClass = String<!>
}

fun foo() {
    <!UNSUPPORTED_FEATURE!>typealias Local = String<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
