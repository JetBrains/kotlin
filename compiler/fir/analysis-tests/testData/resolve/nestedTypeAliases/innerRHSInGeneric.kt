// RUN_PIPELINE_TILL: FRONTEND
// SKIP_FIR_DUMP
// ISSUE: KT-73765

package TAInGeneric

import TAInGeneric.GenericTAOwner.*

class Generic<T> {
    inner class Inner
    fun method(arg: T) = arg
}

open class GenericTAOwner<TO> {

    typealias TA = Generic<<!UNRESOLVED_REFERENCE!>TO<!>>
    typealias TA2 = Generic<GenericTAOwner<<!UNRESOLVED_REFERENCE!>TO<!>>>
    typealias TA3 = <!UNRESOLVED_REFERENCE!>TO<!>

    typealias GenericTA = Generic<Int>
    fun test10(arg: GenericTA): Generic<Int> = arg

    typealias GenericTAWithTP<T> = Generic<T>
    fun <T>test20(): Generic<T> = GenericTAWithTP<T>()

    typealias GenericInnerTA = Generic<Int>.Inner
    fun test40(): Generic<Int>.Inner = Generic<Int>().GenericInnerTA()

    typealias GenericInnerTAWithTP<T> = Generic<T>.Inner
    fun <T>test41(): Generic<T>.Inner =Generic<T>().GenericInnerTAWithTP()

    inner class Inner

    typealias WrongInnerTA = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Inner<!>
    typealias WrongInnerTA2 = GenericTAOwner<<!UNRESOLVED_REFERENCE!>TO<!>>.Inner

    typealias InnerTA = GenericTAOwner<Int>.Inner

    fun test410(): GenericTAOwner<Int>.Inner =  GenericTAOwner<Int>().InnerTA()fun test411(): GenericTAOwner<Int>.Inner = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>InnerTA<!>()
    fun test412(): Inner = <!RETURN_TYPE_MISMATCH!>this.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>InnerTA<!>()<!>
    fun test413(): Inner = <!RETURN_TYPE_MISMATCH!>GenericTAOwner<TO>().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>InnerTA<!>()<!>
    fun test414(): Inner = <!RETURN_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>InnerTA<!>()<!>
    fun test415(): Inner = <!RETURN_TYPE_MISMATCH!>GenericTAOwner<Int>().InnerTA()<!>

    typealias InnerTAWithTP<T> = GenericTAOwner<T>.Inner

    fun test50(): Inner = this.InnerTAWithTP()
    fun test51(): Inner = GenericTAOwner<TO>().InnerTAWithTP()
    fun <T>test52(): Inner = <!RETURN_TYPE_MISMATCH!>this.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>InnerTAWithTP<!><T>()<!>
    fun test53(): Inner = InnerTAWithTP()

    inner class InnerGeneric<TI>

    typealias WrongInnerGenericTA<T> = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>InnerGeneric<T><!>
    typealias WrongInnerGenericTA2<T> = GenericTAOwner<<!UNRESOLVED_REFERENCE!>TO<!>>.InnerGeneric<T>
    typealias InnerGenericTA<T> = GenericTAOwner<T>.InnerGeneric<Int>

    fun <T>test61(): GenericTAOwner<T>.InnerGeneric<Int> = GenericTAOwner<T>().InnerGenericTA()
    fun test62(): InnerGeneric<Int> = this.InnerGenericTA()

    typealias InnerGenericTA2<T> = GenericTAOwner<T>.InnerGeneric<<!UNRESOLVED_REFERENCE!>TO<!>>
    fun <T>test71(): GenericTAOwner<T>.InnerGeneric<TO> = GenericTAOwner<T>().InnerGenericTA2<T>()
    fun test72(): InnerGeneric<TO> = InnerGenericTA2()

    typealias TAtoClassThatHasInnerInTypeArgument = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Generic<Inner><!>
    typealias TAtoClassThatHasTAtoInnerInTypeArgument = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Generic<WrongInnerTA><!>
    typealias TAWithExplicitParameterInTypeArgument = Generic<GenericTAOwner<<!UNRESOLVED_REFERENCE!>TO<!>>.Inner>
    typealias TAWithNonCapturingParameterInTypeArgument<K> = Generic<GenericTAOwner<K>.Inner>
    typealias TAtoInnerAndInnerInTypeArgument<L> = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS("'TO' defined in 'TAInGeneric.GenericTAOwner'")!>InnerGeneric<InnerGeneric<L>><!>
}

typealias GenericTAOwnerInnerTA = GenericTAOwner<Int>.Inner

fun test() {
    GenericTAOwner<Int>().<!UNRESOLVED_REFERENCE!>ClassInnerTA<!>()
    GenericTAOwner<Int>().InnerTA()
    GenericTAOwner<Int>().GenericTAOwnerInnerTA()
    GenericTAOwner<Int>().InnerTAWithTP<Int>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, nullableType, thisExpression, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
