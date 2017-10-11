package codegen.inline.defaultArgs_linkTest_main

import kotlin.test.*

import codegen.inline.defaultArgs_linkTest.a.*

@Test fun runTest() {
    println(foo(5))
    println(foo(5, 42))
}