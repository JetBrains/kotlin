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
            <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int>.BaseTypealias<!>::length
            <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.BaseTypealias<Int><!>::length
            A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int, Int><!>::length
            <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.BaseTypealias<!>::length

            <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<Int>.TypealiasWithTypeParam<Int><!>::foo
            <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.TypealiasWithTypeParam<Int, Int><!>::foo
            A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int, Int, Int><!>::foo
            <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.TypealiasWithTypeParam<Int><!>::foo
        }
    }

    fun test() {
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A<Int>.BaseTypealias<!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int><!><!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Local<Int>.A.BaseTypealias<Int, Int><!>::length
        Local.A.BaseTypealias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int, Int><!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local.A<Int, Int>.BaseTypealias<!>::length
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int, Int>.A.BaseTypealias<!>::length
        <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Local.A.BaseTypealias<!>::length

        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A<Int>.TypealiasWithTypeParam<Int><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int>.A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int, Int><!><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local.A<Int>.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int, Int><!><!>::foo
        Local.A.TypealiasWithTypeParam<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!><Int, Int, Int><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local.A<Int, Int, Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!>TypealiasWithTypeParam<!><!>::foo
        <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Local<Int, Int, Int>.A.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_LOCAL_CLASS_IN_LHS_WARNING!>TypealiasWithTypeParam<!><!>::foo
        <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Local.A.TypealiasWithTypeParam<Int><!>::foo
    }
}

class Z<T> {
    fun foo() {}
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, localFunction,
nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
