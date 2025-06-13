// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    val plainField: Int = 1
    @delegate:Transient
    val lazy by lazy { 1 }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFieldDelegate, classDeclaration, integerLiteral, lambdaLiteral,
propertyDeclaration, propertyDelegate */
