// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects, +ForbidUselessTypeArgumentsIn25,  +ProperSupportOfInnerClassesInCallableReferenceLHS
//                                                                      ^ otherwise, different positioning for one of the diagnostics
// ISSUE: KT-84280, KT-84380

// MODULE: common
expect class MyUnit

expect class SomeObject

expect class WithT<T>

fun testCommon() {
    <!NO_COMPANION_OBJECT!>MyUnit<!>
    <!NO_COMPANION_OBJECT!>MyUnit<Any><!>

    MyUnit::class
    MyUnit::hashCode

    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::hashCode

    <!NO_COMPANION_OBJECT!>SomeObject<!>
    <!NO_COMPANION_OBJECT!>SomeObject<Any><!>

    SomeObject::class
    SomeObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class

    <!NO_COMPANION_OBJECT!>WithT<!>
    <!NO_COMPANION_OBJECT!>WithT<Any><!>

    WithT::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>WithT<!>::hashCode

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>WithT<Any>::class<!>
    WithT<Any>::hashCode

    WithT<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Any><!>::class
    WithT<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Any><!>::hashCode
}

// MODULE: platform()()(common)

actual typealias MyUnit = Unit

object A { fun foo() {} }
actual typealias SomeObject = A

class B<T> {
    companion object
}

actual typealias WithT<T> = B<T>

fun test() {
    MyUnit
    MyUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    MyUnit::class
    MyUnit::hashCode

    MyUnit<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    MyUnit<Any>::<!UNRESOLVED_REFERENCE!>hashCode<!>

    SomeObject
    SomeObject<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    SomeObject::class
    SomeObject::foo

    SomeObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    SomeObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::foo

    WithT
    WithT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    WithT::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>WithT<!>::hashCode

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>WithT<Any>::class<!>
    WithT<Any>::hashCode

    WithT<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Any><!>::class
    WithT<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Any><!>::hashCode
}

/* GENERATED_FIR_TAGS: actual, callableReference, classDeclaration, classReference, expect, functionDeclaration,
objectDeclaration, typeAliasDeclaration */
