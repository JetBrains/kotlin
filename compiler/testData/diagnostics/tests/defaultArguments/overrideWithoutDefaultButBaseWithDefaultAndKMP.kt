// FIR_IDENTICAL
// ISSUE: KT-69870
// LANGUAGE: +MultiPlatformProjects

import O.foo

class C

interface I {
    fun C.foo(s: String = "asdf")
}

object O : I {
    override fun C.foo(s: String) {
    }
}

fun bar(block: C.() -> Unit) {
}

fun test() {
    bar {
        foo()
    }
}
