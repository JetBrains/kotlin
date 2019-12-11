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
import a
import a.b

import a.foo
import a.foo.bar
import a.bar
import a.bar.foo

import a.B.foo
import a.B.foo.bar
import a.B.bar
import a.B.bar.foo

import a.C.foo
import a.C.foo.bar
import a.C.bar
import a.C.bar.foo
import a.C.Nested

import a.D.foo
import a.D.foo.bar
import a.D.bar
import a.D.bar.foo

import a.D.Companion.foo
import a.D.Companion.foo.bar
import a.D.Companion.bar
import a.D.Companion.bar.foo