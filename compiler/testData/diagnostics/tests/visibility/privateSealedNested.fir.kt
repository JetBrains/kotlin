// RUN_PIPELINE_TILL: SOURCE
// FILE: test.kt
import foo.*

fun main() {
    foo.Bar.Baz
    foo.<!INVISIBLE_REFERENCE, NO_COMPANION_OBJECT!>PrivateBar<!>
    foo.PrivateBar.<!INVISIBLE_REFERENCE!>Baz<!>

    PrivateBar.<!INVISIBLE_REFERENCE!>Baz<!>
    <!INVISIBLE_REFERENCE!>PrivateBar<!>.<!INVISIBLE_REFERENCE!>Public<!>()
    PrivateBar.<!INVISIBLE_REFERENCE!>Public<!>.<!INVISIBLE_REFERENCE!>Public<!>()

    Some.<!INVISIBLE_REFERENCE!>Private<!>.<!INVISIBLE_REFERENCE!>Public<!>()
    PrivateInOtherFile.<!INVISIBLE_REFERENCE!>Public<!>.<!INVISIBLE_REFERENCE!>Public<!>()
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
