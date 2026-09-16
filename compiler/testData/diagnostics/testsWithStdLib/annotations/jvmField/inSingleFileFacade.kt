// RUN_PIPELINE_TILL: CODEGEN
@file:JvmName("SomeName")

@JvmField
val c = 4

@JvmField
var g = 5

class C {
    @JvmField
    var g = 5
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, integerLiteral, propertyDeclaration, stringLiteral */
