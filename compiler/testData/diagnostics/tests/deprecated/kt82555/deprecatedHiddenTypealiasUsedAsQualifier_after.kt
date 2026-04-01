// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
//  ^ ignored in K1
// LANGUAGE: +NestedTypeAliases, +SkipHiddenObjectsInResolution
// ISSUE: KT-82555

class C {
    object Impl

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    typealias Obj = Impl

    val obj = Obj
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral,
typeAliasDeclaration */
