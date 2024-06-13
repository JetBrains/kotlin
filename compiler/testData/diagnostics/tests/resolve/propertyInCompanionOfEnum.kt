// CHECK_TYPE
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
    E.<!DEPRECATED_ACCESS_TO_ENUM_COMPANION_PROPERTY!>Entry<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><E>() }
    E.<!DEPRECATED_ACCESS_TO_ENUM_COMPANION_PROPERTY!>Entry<!> checkType { _<String>() }
    E.Companion.Entry checkType { _<String>() }
    E.NotEntry checkType { _<String>() }
    Entry checkType { _<E>() }

    W.Entry checkType { _<String>() }
}

// FILE: Aliased.kt

import test.E as U

fun bar() {
    U.<!DEPRECATED_ACCESS_TO_ENUM_COMPANION_PROPERTY!>Entry<!> checkType { _<<!UNRESOLVED_REFERENCE!>E<!>>() }
    U.<!DEPRECATED_ACCESS_TO_ENUM_COMPANION_PROPERTY!>Entry<!> checkType { _<String>() }
    U.Companion.Entry checkType { _<String>() }
    U.NotEntry checkType { _<String>() }
}
