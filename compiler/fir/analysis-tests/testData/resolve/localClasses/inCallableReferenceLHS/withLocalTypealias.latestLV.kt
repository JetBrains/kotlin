// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344, KT-84407
// LATEST_LV_DIFFERENCE
// LANGUAGE: +LocalTypeAliases

fun localTypealias() {
    class Local<T> {
        inner class A<K> {
            typealias BaseTypealias = String
            typealias TypealiasWithTypeParam<L> = Z<L>
        }
        fun test() {
            <!OUTER_CLASS_ARGUMENTS_REQUIRED, TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>A<Int>.BaseTypealias<!>::length
            A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::length
            A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::length
            <!OUTER_CLASS_ARGUMENTS_REQUIRED!>A.BaseTypealias<!>::length

            <!OUTER_CLASS_ARGUMENTS_REQUIRED, TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>A<Int>.TypealiasWithTypeParam<Int><!>::foo
            A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo
            A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::foo
            <!OUTER_CLASS_ARGUMENTS_REQUIRED!>A.TypealiasWithTypeParam<Int><!>::foo
        }
    }

    fun test() {
        <!OUTER_CLASS_ARGUMENTS_REQUIRED, TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A<Int>.BaseTypealias<!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!><!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!><!>::length
        Local.A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::length
        <!OUTER_CLASS_ARGUMENTS_REQUIRED, TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local.A<Int, Int>.BaseTypealias<!>::length
        <!OUTER_CLASS_ARGUMENTS_REQUIRED, TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int, Int>.A.BaseTypealias<!>::length
        <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Local.A.BaseTypealias<!>::length

        <!OUTER_CLASS_ARGUMENTS_REQUIRED, TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A<Int>.TypealiasWithTypeParam<Int><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local.A<Int>.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!><!>::foo
        Local.A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local.A<Int, Int, Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypealiasWithTypeParam<!><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int, Int, Int>.A.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypealiasWithTypeParam<!><!>::foo
        <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Local.A.TypealiasWithTypeParam<Int><!>::foo
    }
}

class Z<T> {
    fun foo() {}
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, localFunction,
nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
