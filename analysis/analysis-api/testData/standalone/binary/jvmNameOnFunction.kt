// MODULE: lib
// FILE: Test.kt

package test.pkg

object Test {
    @JvmName("notFoo")
    fun foo() {}
}

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import test.pkg.Test

fun test() {
    Test.f<caret>oo()
}