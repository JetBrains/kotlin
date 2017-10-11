package codegen.`object`.fields

import kotlin.test.*

private var globalValue = 1
var global:Int
    get() = globalValue
    set(value:Int) {globalValue = value}

fun globalTest(i:Int):Int {
    global += i
    return global
}


@Test fun runTest() {
    if (global != 1)          throw Error()
    if (globalTest(41) != 42) throw Error()
    if (global != 42)         throw Error()
}