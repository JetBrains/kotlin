// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A, B) -> Unit) { }

fun bar() {
    foo { (<!REDECLARATION, UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>a<!>, <!REDECLARATION!>a<!>), b ->
        a checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><Int>() }
        b checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String>() }
    }

    foo { (<!REDECLARATION, UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>a<!>, b), <!REDECLARATION!>a<!> ->
        a checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><Int>() }
        b checkType { _<String>() }
    }

    foo { <!REDECLARATION!>a<!>, (<!REDECLARATION!>a<!>, b) ->
        a checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><Int>() }
        b checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String>() }
    }

    foo { (a, <!REDECLARATION, UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>), (c, <!REDECLARATION!>b<!>) ->
        a checkType { _<Int>() }
        b checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String>() }
        c checkType { <!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><B>() }
    }
}
