// FIR_IDENTICAL
//  ^ K1 is ignored
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555
// LANGUAGE: +NestedTypeAliases

class Outer {
    class C {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object
    }

    typealias Obj = C

    val obj = <!DEPRECATION_ERROR!>Obj<!>
}

object Obj

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, propertyDeclaration,
stringLiteral, typeAliasDeclaration */
