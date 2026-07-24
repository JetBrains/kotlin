// RUN_PIPELINE_TILL: BACKEND
//  ^ ignored in K1
// LANGUAGE: +NestedTypeAliases
// LANGUAGE_FEATURE_TOGGLED: SkipHiddenObjectsInResolution
// ISSUE: KT-82555

class C {
    object Impl

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    typealias Obj = Impl

    val obj = <!DEPRECATION_ERROR!>Obj<!>
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral,
typeAliasDeclaration */
