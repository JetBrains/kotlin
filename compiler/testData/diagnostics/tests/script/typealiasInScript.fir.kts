// RUN_PIPELINE_TILL: BACKEND
typealias TopLevelInScript = String

class C {
    typealias Nested = String
}

val s1: TopLevelInScript = "TopLevelInScript"
val s2: C.Nested = "C.Nested"

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
