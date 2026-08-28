// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

@file:OptIn(ExperimentalVersionOverloading::class)

class ConstructorNameCollision<!CONFLICTING_JVM_DECLARATIONS!>(
    val value: Int,
    @IntroducedAt("1") val suffix: String = "K",
)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(value: Int) : this(value, "K")<!>
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
