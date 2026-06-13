// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE

class Typealias<K> {
    inner class A<T> {
        typealias BaseTypealias = String
        typealias TypealiasWithTypeParam<L> = Z<L>
    }

    fun usage() {
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>A<String>.BaseTypealias<!>::length
        A.BaseTypealias::length
        A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>A<String>.TypealiasWithTypeParam<String><!>::foo
        A.TypealiasWithTypeParam<String>::foo
        A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>A<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypealiasWithTypeParam<!><!>::foo
    }
}

fun usageTypealias() {
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias<String>.A<String>.BaseTypealias<!>::length
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias<String>.A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!><!>::length
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias<String>.A<String>.TypealiasWithTypeParam<String><!>::foo
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias<String>.A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!><!>::foo
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias<String>.A<String>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypealiasWithTypeParam<!><!>::foo

    Typealias.A.BaseTypealias::length
    Typealias.A.TypealiasWithTypeParam<String>::foo

    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias.A<String>.TypealiasWithTypeParam<String><!>::foo
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Typealias<String>.A.TypealiasWithTypeParam<String><!>::foo
}

class Z<T> {
    fun foo() {}
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, nullableType,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
