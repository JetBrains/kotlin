package codegen.delegatedProperty.simpleVal

import kotlin.test.*

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println(p.name)
        return 42
    }
}

class C {
    val x: Int by Delegate()
}

@Test fun runTest() {
    println(C().x)
}