// RUN_PIPELINE_TILL: FRONTEND
// FILE: test.kt
import foo.*

fun main() {
    foo.Bar.Baz
    foo.<!INVISIBLE_REFERENCE, NO_COMPANION_OBJECT!>PrivateBar<!>
    foo.<!INVISIBLE_REFERENCE!>PrivateBar<!>.<!INVISIBLE_MEMBER!>Baz<!>

    <!INVISIBLE_REFERENCE!>PrivateBar<!>.<!INVISIBLE_MEMBER!>Baz<!>
    <!INVISIBLE_REFERENCE!>PrivateBar<!>.<!INVISIBLE_MEMBER!>Public<!>()
    <!INVISIBLE_REFERENCE!>PrivateBar<!>.<!INVISIBLE_REFERENCE!>Public<!>.<!INVISIBLE_MEMBER!>Public<!>()

    Some.<!INVISIBLE_REFERENCE!>Private<!>.<!INVISIBLE_MEMBER!>Public<!>()
    <!INVISIBLE_REFERENCE!>PrivateInOtherFile<!>.<!INVISIBLE_REFERENCE!>Public<!>.<!INVISIBLE_MEMBER!>Public<!>()
}

class Some {
    private class Private {
        class Public
    }
}

// FILE: otherFile.kt

private class PrivateInOtherFile {
    class Public {
        class Public
    }
}

// FILE: foo.kt
package foo

class Bar {
    object Baz
}

private class PrivateBar {
    object Baz

    class Public {
        class Public
    }
}