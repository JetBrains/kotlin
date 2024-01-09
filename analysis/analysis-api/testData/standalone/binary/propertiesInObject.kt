// MODULE: lib

// FILE: some/DependencyObject.kt
package some

interface Flag<T>

object DependencyObject {
    val VAL_FLAG: Flag<*> = TODO()
    var varFlag: Flag<*> = TODO()
}

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

package some

private fun consumeFlag(p: Flag<*>) {
    println(p)
}

fun test() {
    consumeFlag(DependencyObject.VAL_FLAG)
    consumeFlag(DependencyObject.var<caret>Flag)
}