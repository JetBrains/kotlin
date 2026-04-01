// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344
// LATEST_LV_DIFFERENCE

class Outer<A> {
    inner class Inner<C> {
        fun <T> id(x: T): T = x
    }

    fun test() {
        val a : Inner<Int>.(String)->String = Inner<Int>::id
        val b : Inner<Int>.(String)->String = Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, A><!>::id
        val c : Inner<Int>.(String)->String = Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, A, String><!>::id
    }
}

fun test() {
    val a : Outer<Int>.Inner<Int>.(String)->String = Outer<Int>.Inner<Int>::id
    val b : Outer<Int>.Inner<Int>.(String)->String = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.Inner<Int, Int>::id
    val c : Outer<Int>.Inner<Int>.(String)->String = Outer<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>.Inner::id
    val d : Outer<Int>.Inner<Int>.(String)->String <!INITIALIZER_TYPE_MISMATCH!>=<!> Outer<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>.Inner<String>::<!CANNOT_INFER_PARAMETER_TYPE!>id<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, localProperty,
nullableType, propertyDeclaration, typeParameter, typeWithExtension */
