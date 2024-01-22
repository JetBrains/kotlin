// !DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB

class A {
    class B<T> {
        class C {
            fun foo() {}
        }

        inner class InnerC {
            fun foo() {}
        }

        class ParametricC<K> {
            fun foo() {}
        }
    }
}

fun goodClassifiers() {
    A.B.C::class
    A.B.C::foo

    A.B.InnerC::class
    A.B<Int>.InnerC::foo
}

fun nullableClassifiers() {
    <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>A.B.C?::class<!>
    A.B.C?::<!UNSAFE_CALL!>foo<!>

    <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>A.B.InnerC?::class<!>
    A.B<Int>.InnerC?::<!UNSAFE_CALL!>foo<!>
}

fun classifiersWithTA() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.B<Int>.C<!>::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.B<Int>.C<!>::foo

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.B<Int>.InnerC<!>::class
    // A.B<Int>.InnerC::foo // correct

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A.B.ParametricC<Int>::class<!>
    A.B.ParametricC<Int>::foo
}

val a get() = listOf(10)
val <T> List<T>.b get() = first()
val <T> List<T>.maybeB get() = firstOrNull()
val Int.c get() = A.B.C()
val Int.maybeC: A.B.C? get() = A.B.C()

fun rain() {
    a.b.c::class
    a.b.c::foo

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.b.c<!>?::class
    <!SAFE_CALLABLE_REFERENCE_CALL!>a.b.c?::foo<!>

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.b<!UNNECESSARY_SAFE_CALL!>?.<!>c<!>::class
    a.b<!UNNECESSARY_SAFE_CALL!>?.<!>c::<!UNSAFE_CALL!>foo<!>

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.maybeB?.c<!>::class
    a.maybeB?.c::<!UNSAFE_CALL!>foo<!>

    a.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>b<!><Int>.c::class
    a.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>b<!><Int>.c::foo

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.b.maybeC<!>::class
    a.b.maybeC::<!UNSAFE_CALL!>foo<!>

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.b.maybeC<!>?::class
    <!SAFE_CALLABLE_REFERENCE_CALL!>a.b.maybeC?::<!UNSAFE_CALL!>foo<!><!>
}
