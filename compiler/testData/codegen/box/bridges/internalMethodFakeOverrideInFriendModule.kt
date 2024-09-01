// WITH_STDLIB

// MODULE: lib1

// FILE: Demo.kt
var DemoSetterCalls = 0
var DemoGetterCalls = 0

abstract class Demo {
    internal fun demoFun(): Int = 5
    internal val demoVal: Int = 6
    internal val demoValGet: Int
        get() = 7
    internal var demoVarSetGet: Int = 8
        set(value) { ++DemoSetterCalls; field = value }
        get() { ++DemoGetterCalls; return field }
}

// MODULE: main()(lib1)

// FILE: MyDemo.kt
class MyDemo : Demo()

// FILE: Box.kt
import kotlin.test.assertEquals

fun testMyDemo() {
    val myDemo = MyDemo()
    
    assertEquals(myDemo.demoFun(), 5)
    assertEquals(myDemo.demoVal, 6)
    assertEquals(myDemo.demoValGet, 7)
    assertEquals(myDemo.demoVarSetGet, 8)
    myDemo.demoVarSetGet = -9
    assertEquals(myDemo.demoVarSetGet, -9)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)
}

fun box(): String {
    testMyDemo()
    return "OK"
}
