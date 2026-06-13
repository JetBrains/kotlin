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
        Inner<<!UPPER_BOUND_VIOLATED("Element<K (of class Container<K : Key>)>; Alpha; T (of class Inner<T : Element<K>, Outer(K) : Key>)")!>Alpha<!>>::toString
        Inner<Element<K>>::toString
    }
}

class ImplAlpha : Container<AlphaKey>() {
    private fun foo() {
        Inner<Alpha>::toString
    }
}

class ImplBeta : Container<BetaKey>() {
    private fun foo() {
        Inner<<!UPPER_BOUND_VIOLATED("Element<BetaKey>; Alpha; T (of class Inner<T : Element<K>, Outer(K) : Key>)")!>Alpha<!>>::toString
    }
}

fun foo() {
    Container<AlphaKey>.Inner<Alpha>::toString
    Container<BetaKey>.Inner<<!UPPER_BOUND_VIOLATED("Element<BetaKey>; Alpha; T (of class Inner<T : Element<K>, Outer(K) : Key>)")!>Alpha<!>>::toString
    Container<<!UPPER_BOUND_VIOLATED("Key; String; K (of class Container<K : Key>)")!>String<!>>.Inner<<!UPPER_BOUND_VIOLATED("Element<String>; Alpha; T (of class Inner<T : Element<K>, Outer(K) : Key>)")!>Alpha<!>>::toString
}

fun <K: Key> local() {
    class Local<T: Element<K>>
    Local<<!UPPER_BOUND_VIOLATED("Element<K (of fun <K : Key> local)>; Alpha; T (of class Local<T : Element<K>, Outer(K) : Key>)")!>Alpha<!>>::toString
    Local<Element<K>>::toString
}

open class A<X> {
    inner class B<Y : X> {
        inner class C<Z>
    }
}

class D : A<String>() {
    val refString = B<String>.C<Int>::toString
    val refAny = B<<!UPPER_BOUND_VIOLATED("String; Any; Y (of class B<Y : X, Outer(X)>)")!>Any<!>>.C<Int>::toString
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, interfaceDeclaration, sealed,
typeConstraint, typeParameter */
