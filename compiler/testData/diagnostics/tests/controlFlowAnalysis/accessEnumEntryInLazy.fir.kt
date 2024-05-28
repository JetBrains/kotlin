// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-68339

enum class ECCurve {
    A;

    val a by lazy {
        val bString = when (this) {
            <!UNINITIALIZED_ENUM_ENTRY!>A<!> -> ""
        }
    }

    val b by lazy {
        val bString = <!UNINITIALIZED_ENUM_ENTRY!>A<!>
    }

    val c by lazy {
        fun local() {
            println(<!UNINITIALIZED_ENUM_ENTRY!>A<!>)
        }
    }

    val d by lazy {
        class Local {
            fun foo() {
                println(<!UNINITIALIZED_ENUM_ENTRY!>A<!>)
            }
        }
    }

    val e by lazy {
        object {
            fun foo() {
                println(<!UNINITIALIZED_ENUM_ENTRY!>A<!>)
            }
        }
    }
}
