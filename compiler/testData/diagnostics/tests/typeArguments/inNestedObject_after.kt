// ISSUE: KT-84380, KT-84281
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25,  +ProperSupportOfInnerClassesInCallableReferenceLHS
//                                            ^ otherwise, different positioning for one of the diagnostics

object OutObject {
    object InObject {
        fun foo() {}
    }
}

class OutClass<T> {
    object InObject {
        fun foo() {}
    }
}

typealias OutObjectInObject = OutObject.InObject
typealias OutObjectInObjectWithT<T> = OutObject.InObject
typealias OutClassInObject = OutClass.InObject
typealias OutClassInObjectWithT<T> = OutClass.InObject

fun test() {
    OutObject.InObject
    OutObject.InObject<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    OutObjectInObject
    OutObjectInObject<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    OutObjectInObjectWithT
    OutObjectInObjectWithT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    OutClass.InObject
    OutClass.InObject<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    OutClassInObject
    OutClassInObject<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>
    OutClassInObjectWithT
    OutClassInObjectWithT<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    OutObject.InObject::class
    OutObject.InObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    OutObjectInObject::class
    OutObjectInObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    OutObjectInObjectWithT::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>OutObjectInObjectWithT<Any>::class<!>

    OutObject.InObject::foo
    OutObject.InObject<Any>::<!UNRESOLVED_REFERENCE!>foo<!>
    OutObjectInObject::foo
    OutObjectInObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::foo
    OutObjectInObjectWithT::foo
    OutObjectInObjectWithT<Any>::foo

    OutClass.InObject::class
    OutClass.InObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    OutClassInObject::class
    OutClassInObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    OutClassInObjectWithT::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>OutClassInObjectWithT<<!UNRESOLVED_REFERENCE!>T<!>>::class<!>

    OutClass.InObject::foo
    OutClass.InObject<Any>::<!UNRESOLVED_REFERENCE!>foo<!>
    OutClassInObject::foo
    OutClassInObject<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::foo
    OutClassInObjectWithT::foo
    OutClassInObjectWithT<<!UNRESOLVED_REFERENCE!>T<!>>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, functionDeclaration, nestedClass,
nullableType, objectDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
