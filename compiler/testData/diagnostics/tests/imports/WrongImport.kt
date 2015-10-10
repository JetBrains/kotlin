// FILE:a.kt
package a.b

// FILE:a.kt
package a

val foo = object {
    fun bar() {}
}

fun bar() = object {
    val foo = 239
}

class B {
    val foo = object {
        fun bar() {}
    }

    fun bar() = object {
        val foo = 239
    }
}

object C {
    val foo = object {
        fun bar() {}
    }

    fun bar() = object {
        val foo = 239
    }

    class Nested
}

class D {
    companion object {
        val foo = object {
            fun bar() {}
        }

        fun bar() = object {
            val foo = 239
        }
    }
}


// FILE:b.kt
import <!PACKAGE_CANNOT_BE_IMPORTED!>a<!>
import a.<!PACKAGE_CANNOT_BE_IMPORTED!>b<!>

import a.foo
import a.<!UNRESOLVED_REFERENCE!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
import a.bar
import a.<!UNRESOLVED_REFERENCE!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>

import a.B.<!CANNOT_BE_IMPORTED!>foo<!>
import a.B.<!UNRESOLVED_REFERENCE!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
import a.B.<!CANNOT_BE_IMPORTED!>bar<!>
import a.B.<!UNRESOLVED_REFERENCE!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>

import a.C.foo
import a.C.<!UNRESOLVED_REFERENCE!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
import a.C.bar
import a.C.<!UNRESOLVED_REFERENCE!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
import a.C.Nested

import a.D.<!UNRESOLVED_REFERENCE!>foo<!>
import a.D.<!UNRESOLVED_REFERENCE!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
import a.D.<!UNRESOLVED_REFERENCE!>bar<!>
import a.D.<!UNRESOLVED_REFERENCE!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>

import a.D.Companion.foo
import a.D.Companion.<!UNRESOLVED_REFERENCE!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
import a.D.Companion.bar
import a.D.Companion.<!UNRESOLVED_REFERENCE!>bar<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>