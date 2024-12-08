// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73752

class Outer<T> {
    inner class Inner

    inner class InnerWithParameter<K>

    typealias NestedTAtoExplicitInner = Outer<<!UNRESOLVED_REFERENCE!>T<!>>.Inner // UNRESOLVED_REFERENCE (not inner typealias)
    typealias NestedTAToInner = Inner // Error, not inner type alias can't capture type parameters (they are implicit); TODO: KT-73765
    typealias NestedTAToIntInner = Outer<Int>.Inner // OK
    typealias NestedTAWithTypeParameterToInner<K> = Outer<K>.Inner // OK
    typealias NestedTAtoInnerWithTypeParameters = InnerWithParameter<String> // Error, not inner type alias can't capture type parameters (they are implicit); TODO: KT-73765
    typealias NestedTAtoIntInnerWithTypeParameters = Outer<Int>.InnerWithParameter<String> // OK

    fun test() {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER("Outer<Int>.constructor(): Outer.Inner<Int>")!>TAToIntInner<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)

        NestedTAtoExplicitInner() // Error (minor, not inner typealias)
        NestedTAToInner() // Error (minor, not inner typealias)
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER("Outer<Int>.constructor(): Outer.Inner<Int>")!>NestedTAToIntInner<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER("Outer<K>.constructor<K>(): Outer.Inner<K>")!>NestedTAWithTypeParameterToInner<!><String>() // Error (different dispath receivers `Outer<T>` and `Outer<String>`)
        NestedTAtoInnerWithTypeParameters() // Error (minor, not inner typealias)
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER("Outer<Int>.constructor(): Outer.InnerWithParameter<String, Int>")!>NestedTAtoIntInnerWithTypeParameters<!>() // Error (different dispath receivers `Outer<T>` and `Outer<Int>`)
    }
}

typealias TAToIntInner = Outer<Int>.Inner
