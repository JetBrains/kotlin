// RUN_PIPELINE_TILL: BACKEND
typealias TopLevelInScript = String

class C {
    typealias NestedInClass = String
}

fun foo() {
    typealias Local = String
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
