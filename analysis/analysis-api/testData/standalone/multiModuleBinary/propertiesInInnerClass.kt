// MODULE: lib

// FILE: some/Outer.kt
package some

interface Flag<T>

class Outer {
    val VAL_FLAG: Flag<*> = TODO()
    var varFlag: Flag<*> = TODO()

    inner class Inner {
        val VAL_FLAG: Flag<*> = TODO()
        var varFlag: Flag<*> = TODO()
    }
}

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

package some

private fun consumeFlag(p: Flag<*>) {
    println(p)
}

fun test() {
    val o = Outer()
    consumeFlag(o.VAL_FLAG)
    consumeFlag(o.varFlag)
    val i = o.Inner()
    consumeFlag(i.VAL_FLAG)
    consumeFlag(i.var<caret>Flag)
}