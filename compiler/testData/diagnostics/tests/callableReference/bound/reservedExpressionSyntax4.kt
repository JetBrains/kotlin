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
    A.B.C?::<!TYPE_MISMATCH, UNSAFE_CALL!>foo<!>

    <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>A.B.InnerC?::class<!>
    A.B<Int>.InnerC?::<!TYPE_MISMATCH, UNSAFE_CALL!>foo<!>
}

fun classifiersWithTA() {
    A.B<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.C::class
    A.B<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.C::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A.B<Int>.InnerC::class<!>
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

    <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a.b.c<!>?::class
    <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a.b.c<!>?::foo

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.b<!UNNECESSARY_SAFE_CALL!>?.<!>c<!>::class
    a.b<!UNNECESSARY_SAFE_CALL!>?.<!>c::<!TYPE_MISMATCH, UNSAFE_CALL!>foo<!>

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.maybeB?.c<!>::class
    a.maybeB?.c::<!TYPE_MISMATCH, UNSAFE_CALL!>foo<!>

    <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!><!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>b<!><<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>>.<!DEBUG_INFO_MISSING_UNRESOLVED!>c<!><!>::class
    <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!><!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>b<!><<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>>.<!DEBUG_INFO_MISSING_UNRESOLVED!>c<!><!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>a.b.maybeC<!>::class
    a.b.maybeC::<!TYPE_MISMATCH, UNSAFE_CALL!>foo<!>

    <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS, RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a.b.maybeC<!>?::class
    <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a.b.maybeC<!>?::<!TYPE_MISMATCH, UNSAFE_CALL!>foo<!>
}
