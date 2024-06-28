// DUMP_CFG
inline fun <T> myRun(block: () -> T) = block()

fun test(a: Any, b: Any) {
    if (a !is String) return
    // (1)
    class A {
        fun foo() = myRun {
            a.length
            if (b is String) {
                b.length
                bar()
            } else {
                1
            }
        }

        fun bar() = myRun {
            b.<!UNRESOLVED_REFERENCE!>length<!>
            a.length
            baz()
        }

        fun baz() = 1
    }

    val x = object {
        fun foo() = myRun {
            a.length
            if (b is String) {
                b.length
                bar()
            } else {
                1
            }
        }

        fun bar() = myRun {
            a.length
            b.<!UNRESOLVED_REFERENCE!>length<!>
            baz()
        }

        fun baz() = 1
    }
}
