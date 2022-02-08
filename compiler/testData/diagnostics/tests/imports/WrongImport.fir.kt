// FILE:a.kt
package a.b

// FILE:b.kt
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


// FILE:c.kt
import <!PACKAGE_CANNOT_BE_IMPORTED!>a<!>
import a.<!PACKAGE_CANNOT_BE_IMPORTED!>b<!>

import a.foo
import a.<!UNRESOLVED_IMPORT!>foo<!>.bar
import a.bar
import a.<!UNRESOLVED_IMPORT!>bar<!>.foo

import a.B.<!CANNOT_BE_IMPORTED!>foo<!>
import a.B.<!UNRESOLVED_IMPORT!>foo<!>.bar
import a.B.<!CANNOT_BE_IMPORTED!>bar<!>
import a.B.<!UNRESOLVED_IMPORT!>bar<!>.foo

import a.C.foo
import a.C.<!UNRESOLVED_IMPORT!>foo<!>.bar
import a.C.bar
import a.C.<!UNRESOLVED_IMPORT!>bar<!>.foo
import a.C.Nested

import a.D.<!UNRESOLVED_IMPORT!>foo<!>
import a.D.<!UNRESOLVED_IMPORT!>foo<!>.bar
import a.D.<!UNRESOLVED_IMPORT!>bar<!>
import a.D.<!UNRESOLVED_IMPORT!>bar<!>.foo

import a.D.Companion.foo
import a.D.Companion.<!UNRESOLVED_IMPORT!>foo<!>.bar
import a.D.Companion.bar
import a.D.Companion.<!UNRESOLVED_IMPORT!>bar<!>.foo
