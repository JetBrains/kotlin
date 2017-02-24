package stopInCrossinlineInSuspend

import forTests.builder

fun main(args: Array<String>) {
    val a = 12

    builder {
        ci {
            {
                //Breakpoint!
                foo(a)
            }()
        }
    }
}

fun foo(a: Any? = null) {}

inline fun ci(crossinline builder: () -> Unit) {
    builder()
}