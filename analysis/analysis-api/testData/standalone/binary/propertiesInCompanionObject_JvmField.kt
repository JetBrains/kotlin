// MODULE: lib

// FILE: some/Dependency.kt
package some

interface Flag<T>

class Dependency {
    companion object {
        @JvmField val JVM_FIELD_FLAG: Flag<*> = TODO()
        @JvmStatic val JVM_STATIC_FLAG: Flag<*> = TODO()
        val VAL_FLAG: Flag<*> = TODO()
        var varFlag: Flag<*> = TODO()
    }
}

val DEPENDENCY_TOP_LEVEL_VAL_FLAG: Flag<*> = TODO()

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

package some

private fun consumeFlag(p: Flag<*>) {
    println(p)
}

fun test() {
    consumeFlag(Dependency.JVM_<caret>FIELD_FLAG)
    consumeFlag(Dependency.JVM_STATIC_FLAG)
    consumeFlag(Dependency.VAL_FLAG)
    consumeFlag(Dependency.varFlag)
    consumeFlag(DEPENDENCY_TOP_LEVEL_VAL_FLAG)
}