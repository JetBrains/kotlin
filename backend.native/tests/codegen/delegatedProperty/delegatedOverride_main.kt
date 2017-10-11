package codegen.delegatedProperty.delegatedOverride_main

import kotlin.test.*

import codegen.delegatedProperty.delegatedOverride.a.*

open class C: B() {
    override val x: Int = 156

    fun foo() {
        println(x)

        println(super<B>.x)
        bar()
    }
}

@Test fun runTest() {
    val c = C()
    c.foo()
}
