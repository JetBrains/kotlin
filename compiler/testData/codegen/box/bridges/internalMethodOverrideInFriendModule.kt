// WITH_STDLIB

// MODULE: lib1

// FILE: Demo.kt
var DemoOpenSetterCalls = 0
var DemoOpenGetterCalls = 0

abstract class Demo {
    internal open fun demoOpenFun(): Int = 5
    internal open val demoOpenVal: Int = 6
    internal open val demoOpenValGet: Int
        get() = 7
    internal open var demoOpenVarSetGet: Int = 8
        set(value) { ++DemoOpenSetterCalls; field = value }
        get() { ++DemoOpenGetterCalls; return field }
}

// FILE: TestDemo.kt
fun testOpenFun(d: Demo): Int = d.demoOpenFun()
fun testOpenVal(d: Demo): Int = d.demoOpenVal
fun testOpenValGet(d: Demo): Int = d.demoOpenValGet
fun testOpenVarGet(d: Demo): Int = d.demoOpenVarSetGet
fun testOpenVarSet(d: Demo, v: Int) { d.demoOpenVarSetGet = v }

// MODULE: main()(lib1)

// FILE: MyDemo.kt
var MyDemoOpenSetterCalls = 0
var MyDemoOpenGetterCalls = 0

class MyDemo : Demo() {
    override fun demoOpenFun(): Int = 100 * super.demoOpenFun()
    override val demoOpenVal: Int = 600
    override val demoOpenValGet: Int
        get() = 110 * super.demoOpenValGet
    override var demoOpenVarSetGet: Int
        set(value) { ++MyDemoOpenSetterCalls; super.demoOpenVarSetGet = value }
        get() { ++MyDemoOpenGetterCalls; return 111 * super.demoOpenVarSetGet }
}

// FILE: Box.kt
import kotlin.test.assertEquals

fun testMyDemo() {
    val myDemo = MyDemo()

    assertEquals(testOpenFun(myDemo), 500)
    assertEquals(testOpenVal(myDemo), 600)
    assertEquals(testOpenValGet(myDemo), 770)
    assertEquals(testOpenVarGet(myDemo), 888)
    testOpenVarSet(myDemo, -8)
    assertEquals(testOpenVarGet(myDemo), -888)
    assertEquals(DemoOpenSetterCalls, 1)
    assertEquals(DemoOpenGetterCalls, 2)
    assertEquals(MyDemoOpenSetterCalls, 1)
    assertEquals(MyDemoOpenGetterCalls, 2)

    assertEquals(myDemo.demoOpenFun(), 500)
    assertEquals(myDemo.demoOpenVal, 600)
    assertEquals(myDemo.demoOpenValGet, 770)
    assertEquals(myDemo.demoOpenVarSetGet, -888)
    myDemo.demoOpenVarSetGet = -9
    assertEquals(myDemo.demoOpenVarSetGet, -999)
    assertEquals(DemoOpenSetterCalls, 2)
    assertEquals(DemoOpenGetterCalls, 4)
    assertEquals(MyDemoOpenSetterCalls, 2)
    assertEquals(MyDemoOpenGetterCalls, 4)
}

fun box(): String {
    testMyDemo()
    return "OK"
}
