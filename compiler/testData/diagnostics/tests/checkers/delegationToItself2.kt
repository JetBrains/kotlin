// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: -ForbidObjectDelegationToItself
// ISSUE: KT-17417, KT-46313

interface SomeInterface {
    val x : Int
}

enum class EEE {
    A, B, C;

    companion <!ABSTRACT_MEMBER_INCORRECTLY_DELEGATED_WARNING("Object 'EEE.Companion'; member:In package 'root package' of module '<main>':  --> delegationToItself2.kt   | 9 | enum class EEE : Enum<EEE> {   | ...12 |     companion object Companion : SomeInterface {   | ...12 |         val x: Int   | ...   | }   |")!>object<!> : SomeInterface by EEE
}

class Some {
    companion <!ABSTRACT_MEMBER_INCORRECTLY_DELEGATED_WARNING!>object<!> : SomeInterface by Some
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, inheritanceDelegation,
interfaceDeclaration, objectDeclaration, propertyDeclaration */
