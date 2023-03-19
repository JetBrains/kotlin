// LANGUAGE: -SafeCallsAreAlwaysNullable
// DIAGNOSTICS: -UNNECESSARY_SAFE_CALL
// ISSUE: KT-46860

interface A {
    fun id(): A

    fun foo(): String
}

fun test_1(a: A) {
    val s = a.id().id().id().id().id().id().id().id()<!SAFE_CALL_WILL_CHANGE_NULLABILITY!>?.foo()<!>.length
}

fun test_2(a: A) {
    val s = a.id()
        .id()
        .id()
        .id()
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>?.id()<!>
        .id()
        .id()
        .id()
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>?.foo()<!>
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>?.length<!>
}

fun test_3(a: A) {
    val s = a.id()<!SAFE_CALL_WILL_CHANGE_NULLABILITY!>?.
        id()<!>.
        id()<!SAFE_CALL_WILL_CHANGE_NULLABILITY!>?.
        foo()<!>.
        length
}
