// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// LATEST_LV_DIFFERENCE
// ISSUE: KT-82122

// MODULE: common
expect class A <T> {
    inner class B<K> {
        fun foo()
    }
}

expect class B<T>

expect class C<K> {
    inner class D<T>
}

expect fun C<String>.D<Int>.foo()

fun usageCommon() {
    A<String>.B<Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<!>.B<Int, String>::foo
    A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>.B::foo
}

// MODULE: platform()()(common)
actual class A <T> {
    actual inner class B<K> {
        actual fun foo() {}
    }
}

actual class B<T> {
    inner class C<K> {
        fun foo() {}
    }
}

actual class C<K> {
    actual inner class D<T>
}

actual fun C<String>.D<Int>.foo() {}

fun usage() {
    A<String>.B<Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<!>.B<Int, String>::foo
    A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>.B::foo

    B<String>.C<Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<!>.C<Int, Int>::foo
    B<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>.C::foo

    C<String>.D<Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>.D<Int, Int>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>.D::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
}

/* GENERATED_FIR_TAGS: actual, callableReference, classDeclaration, expect, funWithExtensionReceiver,
functionDeclaration, inner, nullableType, typeParameter */
