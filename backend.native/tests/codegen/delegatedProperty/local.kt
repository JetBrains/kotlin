package codegen.delegatedProperty.local

import kotlin.test.*

import kotlin.reflect.KProperty

fun foo(): Int {
   class Delegate {
        operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
            println(p.name)
            return 42
        }
    }

    val x: Int by Delegate()

    return x
}

@Test fun runTest() {
    println(foo())
}