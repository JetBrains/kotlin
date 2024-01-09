// MODULE: lib

// FILE: some/Outer.kt
package some

interface Flag<T>

class Outer {
    val VAL_FLAG: Flag<*> = TODO()
    var varFlag: Flag<*> = TODO()

    object O {
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
    consumeFlag(Outer.O.VAL_FLAG)
    consumeFlag(Outer.O.var<caret>Flag)
}