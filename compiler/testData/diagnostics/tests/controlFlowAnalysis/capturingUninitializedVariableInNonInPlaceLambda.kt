// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

import kotlin.contracts.*

fun capture(block: () -> Unit): String = ""

@OptIn(ExperimentalContracts::class)
inline fun inPlace(block: () -> Unit): String {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return ""
}

fun consume(x: Any?) {}

class A {
    val a = capture { consume(x) }

    val b = inPlace {
        consume(x) // error
        capture { consume(x) } // ok
        inPlace {
            consume(x) // error
            capture { consume(x) } // ok
        }
    }

    val c = object {
        fun foo() {
            consume(x) // ok
            capture { consume(x) } // ok
            inPlace {
                consume(x) // ok
                capture { consume(x) } // ok
            }
        }

        init {
            consume(<!UNINITIALIZED_VARIABLE!>x<!>) // error
            capture { consume(x) } // ok
            inPlace {
                consume(x) // error
                capture { consume(x) } // ok
            }
        }

        val objectProp = inPlace {
            consume(x) // error
            capture { consume(x) } // ok
            inPlace {
                consume(x) // error
                capture { consume(x) } // ok
            }
        }
    }

    val d = inPlace {
        fun localFun() {
            consume(x) // ok
        }

        capture {
            localFun()
        }
    }

    val x = 10
}
