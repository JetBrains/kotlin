// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

open class A<T>
open class B<T>

typealias AliasWithTypeParam<T> = B<T>
typealias Alias = B<*>

fun test1(a: A<Int>){
    <!USELESS_IS_CHECK!>a is B<*><!>
}

fun test2(a: A<*>){
    <!USELESS_IS_CHECK!>a is B<*><!>
}

fun test3(a: A<Int>) {
    <!USELESS_IS_CHECK!>a is AliasWithTypeParam<*><!>
}

fun test4(a: A<Int>) {
    <!USELESS_IS_CHECK!>a is Alias<!>
}

fun <T : A<T>> test5(x: T) {
    x is B<*>
}

fun <T> test6(x: T) where T : A<T>, T : CharSequence {
    x is B<*>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, starProjection, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
