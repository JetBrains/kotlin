// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87505

@JvmInline
value class Some(val x: Int) {
    constructor(
        x: String,
        @IntroducedAt("1") y: Int = 1
    ) : this(x.length + y)
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationUseSiteTargetFile, classDeclaration, classReference, integerLiteral,
primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral, value */
