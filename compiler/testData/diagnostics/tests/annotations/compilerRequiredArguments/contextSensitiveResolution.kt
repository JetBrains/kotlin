// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-86556

@Target(FIELD)
annotation class ViaContextSensitiveResolution

class C {
    @ViaContextSensitiveResolution
    val b = <!DEPRECATION_ERROR!>c<!> + <!UNRESOLVED_REFERENCE!>d<!>
}

@Deprecated("", level = ERROR)
val c = 42

@Deprecated("", ReplaceWith(""), HIDDEN)
val d = 42

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, propertyDeclaration,
typeAliasDeclaration */
