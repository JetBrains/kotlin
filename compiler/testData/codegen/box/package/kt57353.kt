// MODULE: lib
// FILE: lib.kt
package my.lib.pkg

object Foo {
    fun ok() = "OK"
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String = my.lib.pkg.Foo.ok()
