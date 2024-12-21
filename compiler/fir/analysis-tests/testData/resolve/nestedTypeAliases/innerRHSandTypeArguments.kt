// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73765

class C<L>

class Outer<T> {
    inner class Inner

    inner class InnerWithParameter<K> {
        inner class InnerInsideInner

        typealias TAtoInnerInsideInner = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS("'K' defined in '/Outer.InnerWithParameter', 'T' defined in '/Outer'")!>InnerInsideInner<!> // Error, type alias RHS can't capture outer type parameters (they are implicit)
    }

    typealias TAtoExplicitInner = Outer<<!UNRESOLVED_REFERENCE!>T<!>>.Inner // Error, UNRESOLVED_REFERENCE `T`
    typealias TAToInner = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS("'T' defined in '/Outer'")!>Inner<!> // Error, type alias RHS can't capture outer type parameters (they are implicit)
    typealias TAToIntInner = Outer<Int>.Inner // OK
    typealias TAWithTypeParameterToInner<K> = Outer<K>.Inner // OK
    typealias TAtoInnerWithTypeParameters = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS("'T' defined in '/Outer'")!>InnerWithParameter<String><!> // Error, type alias RHS can't capture outer type parameters (they are implicit)
    typealias TAtoIntInnerWithTypeParameters = Outer<Int>.InnerWithParameter<String> // OK

    typealias TAtoTA = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS("'T' defined in '/Outer'")!>TAToInner<!> // Error, type alias RHS can't capture outer type parameters

    typealias TAtoClassThatHasInnerInTypeArgument = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>C<Inner><!> // Error
    typealias TAtoClassThatHasTAtoInnerInTypeArgument = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>C<TAToInner><!> // Error
    typealias TAWithExplicitParameterInTypeArgument = C<Outer<<!UNRESOLVED_REFERENCE!>T<!>>.Inner> // Error (unresolved on `T`)
    typealias TAWithNonCapturingParameterInTypeArgument<K> = C<Outer<K>.Inner> // OK
    typealias TAtoInnerAndInnerInTypeArgument<L> = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS("'T' defined in '/Outer'")!>InnerWithParameter<InnerWithParameter<L>><!> // Error, `T` is captured (avoid `T` duplicaiton)

    fun test() {
        TAtoExplicitInner()
        TAToInner()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAToIntInner<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAWithTypeParameterToInner<!><String>() // Error (different dispath receivers `Outer<T>` and `Outer<String>`)
        TAtoInnerWithTypeParameters()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoIntInnerWithTypeParameters<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)
    }
}

