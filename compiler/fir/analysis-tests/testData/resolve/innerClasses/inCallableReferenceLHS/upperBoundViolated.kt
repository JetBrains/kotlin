// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344
// RENDER_DIAGNOSTIC_ARGUMENTS
// LATEST_LV_DIFFERENCE

sealed interface Key
class AlphaKey : Key
class BetaKey : Key

sealed interface Element<K : Key>
class Alpha : Element<AlphaKey>

open class Container<K: Key> {
    inner class Inner<T: Element<K>>

    private fun foo() {
        <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; inner class Inner<T : Element<K>, Outer(K) : Key> : Any")!>Inner<Alpha><!>::toString
        <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; inner class Inner<T : Element<K>, Outer(K) : Key> : Any")!>Inner<Element<K>><!>::toString
    }
}

class ImplAlpha : Container<AlphaKey>() {
    private fun foo() {
        <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; inner class Inner<T : Element<K>, Outer(K) : Key> : Any")!>Inner<Alpha><!>::toString
    }
}

class ImplBeta : Container<BetaKey>() {
    private fun foo() {
        <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; inner class Inner<T : Element<K>, Outer(K) : Key> : Any")!>Inner<Alpha><!>::toString
    }
}

fun foo() {
    Container<AlphaKey>.Inner<Alpha>::toString
    Container<BetaKey>.Inner<<!UPPER_BOUND_VIOLATED("Element<BetaKey>; Alpha")!>Alpha<!>>::toString
    Container<<!UPPER_BOUND_VIOLATED("Key; String")!>String<!>>.Inner<<!UPPER_BOUND_VIOLATED("Element<String>; Alpha")!>Alpha<!>>::toString
}

fun <K: Key> local() {
    class Local<T: Element<K>>
    Local<<!UPPER_BOUND_VIOLATED("Element<K (of fun <K : Key> local)>; Alpha")!>Alpha<!>>::toString
    Local<Element<K>>::toString
}

open class A<X> {
    inner class B<Y : X> {
        inner class C<Z>
    }
}

class D : A<String>() {
    val refString = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class C<Z, Outer(Y) : X, Outer(X)> : Any")!>B<String>.C<Int><!>::toString
    val refAny = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class C<Z, Outer(Y) : X, Outer(X)> : Any")!>B<Any>.C<Int><!>::toString
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, interfaceDeclaration, sealed,
typeConstraint, typeParameter */
