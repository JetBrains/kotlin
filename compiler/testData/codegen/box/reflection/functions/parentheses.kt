// TARGET_BACKEND: JVM_IR
// IGNORE_DEXING

// WITH_REFLECT

data class `)))`(val value: Int)
interface `)` {
    fun f(i1: `)))` = `)))`(1)): Int
}

data class `))`(val x: Int): `)` {
    override fun f(i1: `)))`) = x + i1.value
}

fun box(): String = if (`))`(2)::f.callBy(mapOf()) == 3) "OK" else "NOT OK"
