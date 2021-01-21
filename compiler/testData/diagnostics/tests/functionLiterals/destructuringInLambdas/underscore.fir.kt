// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A) -> Unit) { }

fun bar() {
    foo { (_, b) ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
        b checkType { _<String>() }
    }

    foo { (a, _) ->
        a checkType { _<Int>() }
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
    }

    foo { (_, _) ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
    }

    foo { (_: Int, b: String) ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
        b checkType { _<String>() }
    }

    foo { (a: Int, _: String) ->
        a checkType { _<Int>() }
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
    }

    foo { (_: Int, _: String) ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
    }

    foo { (_, _): A ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
    }

    foo { (`_`, _) ->
        _ checkType { _<Int>() }
    }

    foo { (_, `_`) ->
        _ checkType { _<String>() }
    }

    foo { (`_`, `_`) ->
        _ checkType { _<String>() }
    }

    foo { (_: String, b) ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
        b checkType { _<String>() }
    }

    <!INAPPLICABLE_CANDIDATE!>foo<!> { (_, b): B ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
        b checkType { _<Short>() }
    }
}
