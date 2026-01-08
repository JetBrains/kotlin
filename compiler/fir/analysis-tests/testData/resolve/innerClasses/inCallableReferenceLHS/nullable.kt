// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344

class A<X> {
    inner open class B<Y> {
        inner class C<Z> {
            fun foo() {
            }
        }

        private val ref = C<Int>?::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>
    }

    inner class StringB : B<String>() {
        val ref = C<Int>?::bar
    }

    val ref1 = B.C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>?::<!UNRESOLVED_REFERENCE!>foo<!>
    val ref2 = B<String>.C<Int>?::<!UNSAFE_CALLABLE_REFERENCE!>foo<!>

    val ref3 = B<String>.C<Int>?::bar
}

fun <T> A<T>.B<String>.C<Int>?.bar() {
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inner, nullableType,
propertyDeclaration, typeParameter */
