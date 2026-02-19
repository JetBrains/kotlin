// MODULE: lib
// FILE: Test.kt

package test.pkg

val Int.prop: Int
  @JvmName("ownPropGetter")
  get() = this * 31

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import test.pkg.*

fun test() {
    42.pro<caret>p
}