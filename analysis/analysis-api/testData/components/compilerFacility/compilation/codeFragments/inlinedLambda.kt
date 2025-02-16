// MODULE: context

// FILE: lib/Lib.java
package lib;

public interface Lib {
    public static void perform(Runnable p0) {
        p0.run();
    }
}

// FILE: lib/kotlinAdapter.kt
package lib

inline fun perform(crossinline p0: () -> Unit) {
    Lib.perform(Runnable { p0() })
}

inline fun unrelatedPerform(crossinline p0: () -> Unit) {
    Lib.perform(Runnable { p0() })
}

fun unrelatedPerformUsage() {
    unrelatedPerform { unrelatedPerformUsage() }
}

// FILE: context.kt
package app

import lib.*

fun test() {
    <caret_context>perform { call() }
}

fun call() {}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
perform { call() }