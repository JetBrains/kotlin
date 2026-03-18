// ISSUE: KT-84380
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25

fun <T, U> test() {
    class Outer<X> {
        inner class Inner
        inner class GenericInner<Y>
    }

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Outer<Int>::class<!>
    Outer<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>::class
    Outer::class

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Outer<Int>.Inner::class<!>
    Outer.Inner::class
    Outer.Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class
    Outer<Int>.Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::class

    Outer<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericInner<!>::class
    Outer.GenericInner::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.GenericInner<Int>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Outer<Int>.GenericInner<String>::class<!>
    Outer.GenericInner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>::class
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, inner, localClass, nullableType,
typeParameter */
