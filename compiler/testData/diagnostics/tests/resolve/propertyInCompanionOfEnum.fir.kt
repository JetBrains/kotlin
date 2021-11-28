// !CHECK_TYPE
// SKIP_TXT

// FILE: test.kt
package test
enum class E {
    Entry;
    companion object {
        val Entry = ""
        val NotEntry = ""
    }
}

// FILE: main.kt

import test.E.Entry
import test.E.Companion as W
import test.E as U
import test.E

fun foo() {
    E.Entry checkType { _<E>() }
    E.Entry checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
    E.Companion.Entry checkType { _<String>() }
    E.NotEntry checkType { _<String>() }
    Entry checkType { _<E>() }

    W.Entry checkType { _<String>() }
}

// FILE: Aliased.kt

import test.E as U

fun bar() {
    U.Entry checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><<!UNRESOLVED_REFERENCE!>E<!>>() }
    U.Entry checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
    U.Companion.Entry checkType { _<String>() }
    U.NotEntry checkType { _<String>() }
}
