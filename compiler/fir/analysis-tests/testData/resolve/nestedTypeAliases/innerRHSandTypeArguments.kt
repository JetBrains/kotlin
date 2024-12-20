// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73765

class Outer<T> {
    inner class Inner

    inner class InnerWithParameter<K> {
        inner class InnerInsideInner

        typealias TAtoInnerInsideInner = InnerInsideInner // Error, type alias RHS can't capture outer type parameters (they are implicit)
    }

    typealias TAtoExplicitInner = Outer<<!UNRESOLVED_REFERENCE!>T<!>>.Inner // Error, UNRESOLVED_REFERENCE `T`
    typealias TAToInner = Inner // Error, type alias RHS can't capture outer type parameters (they are implicit)
    typealias TAToIntInner = Outer<Int>.Inner // OK
    typealias TAWithTypeParameterToInner<K> = Outer<K>.Inner // OK
    typealias TAtoInnerWithTypeParameters = InnerWithParameter<String> // Error, type alias RHS can't capture outer type parameters (they are implicit)
    typealias TAtoIntInnerWithTypeParameters = Outer<Int>.InnerWithParameter<String> // OK

    typealias TAtoTA = TAToInner // Error, type alias RHS can't capture outer type parameters

    fun test() {
        TAtoExplicitInner()
        TAToInner()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAToIntInner<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAWithTypeParameterToInner<!><String>() // Error (different dispath receivers `Outer<T>` and `Outer<String>`)
        TAtoInnerWithTypeParameters()
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoIntInnerWithTypeParameters<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)
    }
}

