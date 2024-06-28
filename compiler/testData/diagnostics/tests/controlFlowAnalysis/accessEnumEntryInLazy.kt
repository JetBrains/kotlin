// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-68339

enum class ECCurve {
    A;

    val a by lazy {
        val bString = when (this) {
            A -> ""
        }
    }

    val b by lazy {
        val bString = A
    }

    val c by lazy {
        fun local() {
            println(A)
        }
    }

    val d by lazy {
        class Local {
            fun foo() {
                println(A)
            }
        }
    }

    val e by lazy {
        object {
            fun foo() {
                println(A)
            }
        }
    }
}
