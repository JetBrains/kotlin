// MODULE: lib
// FILE: Test.kt

package test.pkg

class Test(
    var x: Int
) {
}

var Test.ext: Int
    get() = this.x
    @JvmName("ownPropSetter")
    set(value) {
        this.x = value
    }

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import test.pkg.*

fun test(t: Test) {
    t.ex<caret>t = 42
}