// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87505

@file:OptIn(ExperimentalVersionOverloading::class)

@JvmInline
value class Some(val x: Int) {
    constructor(
        x: String,
        <!INVALID_VERSIONING_ON_VALUE_CLASS_PARAMETER!>@IntroducedAt("1")<!> y: Int = 1
    ) : this(x.length + y)
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationUseSiteTargetFile, classDeclaration, classReference, integerLiteral,
primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral, value */
