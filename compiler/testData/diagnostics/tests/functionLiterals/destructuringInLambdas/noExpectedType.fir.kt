// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val x: Int, val y: String)

fun bar() {
    val x = { (<!UNRESOLVED_REFERENCE!>a<!>, <!UNRESOLVED_REFERENCE!>b<!>): A ->
        a <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    }

    x checkType { <!UNRESOLVED_REFERENCE!>_<!><(A) -> Unit>() }

    val y = { (<!UNRESOLVED_REFERENCE!>a: Int<!>, <!UNRESOLVED_REFERENCE!>b<!>): A ->
        a checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    }

    y checkType { <!UNRESOLVED_REFERENCE!>_<!><(A) -> Unit>() }

    val y2 = { (<!UNRESOLVED_REFERENCE!>a: Number<!>, <!UNRESOLVED_REFERENCE!>b<!>): A ->
        a checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    }

    y2 checkType { <!UNRESOLVED_REFERENCE!>_<!><(A) -> Unit>() }

    val z = { (<!UNRESOLVED_REFERENCE!>a: Int<!>, <!UNRESOLVED_REFERENCE!>b: String<!>) ->
        a checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
        b checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    }
}
