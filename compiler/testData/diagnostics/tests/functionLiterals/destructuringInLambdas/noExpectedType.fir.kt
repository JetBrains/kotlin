// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val x: Int, val y: String)

fun bar() {
    val x = { (<!UNRESOLVED_REFERENCE!>a<!>, <!UNRESOLVED_REFERENCE!>b<!>): A ->
        a <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    x checkType { _<(A) -> Unit>() }

    val y = { (<!UNRESOLVED_REFERENCE!>a: Int<!>, <!UNRESOLVED_REFERENCE!>b<!>): A ->
        a checkType { _<Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    y checkType { _<(A) -> Unit>() }

    val y2 = { (<!UNRESOLVED_REFERENCE!>a: Number<!>, <!UNRESOLVED_REFERENCE!>b<!>): A ->
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    y2 checkType { _<(A) -> Unit>() }

    val z = { (<!UNRESOLVED_REFERENCE!>a: Int<!>, <!UNRESOLVED_REFERENCE!>b: String<!>) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }
}
