// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
//  ^ ignored in K1
// LANGUAGE: +NestedTypeAliases
// ISSUE: KT-82555

@Deprecated("", level = DeprecationLevel.HIDDEN)
object Impl

class C {
    typealias Obj = <!DEPRECATION_ERROR!>Impl<!>

    val obj = <!DEPRECATION_ERROR!>Obj<!>
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral,
typeAliasDeclaration */
