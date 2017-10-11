package codegen.delegatedProperty.packageLevel

import kotlin.test.*

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println(p.name)
        return 42
    }
}

val x: Int by Delegate()

@Test fun runTest() {
    println(x)
}