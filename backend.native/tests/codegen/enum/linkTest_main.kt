package codegen.enum.linkTest_main

import kotlin.test.*

import codegen.enum.linkTest.a.*

@Test fun runTest() {
    println(A.Z1.x)
    println(A.valueOf("Z2").x)
    println(A.values()[2].x)
}