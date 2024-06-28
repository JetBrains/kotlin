// WITH_STDLIB

// MODULE: lib1

// FILE: Demo.kt
var DemoSetterCalls = 0
var DemoGetterCalls = 0
var DemoOpenSetterCalls = 0
var DemoOpenGetterCalls = 0

abstract class Demo {
    internal fun demoFun(): Int = 1
    internal val demoVal: Int = 2
    internal val demoValGet: Int
        get() = 3
    internal var demoVarSetGet: Int = 4
        set(value) { ++DemoSetterCalls; field = value }
        get() { ++DemoGetterCalls; return field }

    internal open fun demoOpenFun(): Int = 5
    internal open val demoOpenVal: Int = 6
    internal open val demoOpenValGet: Int
        get() = 7
    internal open var demoOpenVarSetGet: Int = 8
        set(value) { ++DemoOpenSetterCalls; field = value }
        get() { ++DemoOpenGetterCalls; return field }
}

// FILE: LibDemo.kt
var LibDemoOpenSetterCalls = 0
var LibDemoOpenGetterCalls = 0

open class LibDemo : Demo() {
    override public fun demoOpenFun(): Int = 50
    override public val demoOpenVal: Int = 60
    override public val demoOpenValGet: Int
        get() = 70
    override public var demoOpenVarSetGet: Int = 80
        set(value) { ++LibDemoOpenSetterCalls; field = value }
        get() { ++LibDemoOpenGetterCalls; return field }
}

// FILE: TestDemo.kt
fun testFun(d: Demo): Int = d.demoFun()
fun testVal(d: Demo): Int = d.demoVal
fun testValGet(d: Demo): Int = d.demoValGet
fun testVarGet(d: Demo): Int = d.demoVarSetGet
fun testVarSet(d: Demo, v: Int) { d.demoVarSetGet = v }

fun testOpenFun(d: Demo): Int = d.demoOpenFun()
fun testOpenVal(d: Demo): Int = d.demoOpenVal
fun testOpenValGet(d: Demo): Int = d.demoOpenValGet
fun testOpenVarGet(d: Demo): Int = d.demoOpenVarSetGet
fun testOpenVarSet(d: Demo, v: Int) { d.demoOpenVarSetGet = v }

// MODULE: main(lib1)

// FILE: MyDemo.kt
var MyDemoSetterCalls = 0
var MyDemoGetterCalls = 0
var MyDemoOpenSetterCalls = 0
var MyDemoOpenGetterCalls = 0

class MyDemo : Demo() {
    fun demoFun(): Int = 100
    val demoVal: Int = 200
    val demoValGet: Int
        get() = 300
    var demoVarSetGet: Int = 400
        set(value) { ++MyDemoSetterCalls; field = value }
        get() { ++MyDemoGetterCalls; return field }

    fun demoOpenFun(): Int = 500
    val demoOpenVal: Int = 600
    val demoOpenValGet: Int
        get() = 700
    var demoOpenVarSetGet: Int = 800
        set(value) { ++MyDemoOpenSetterCalls; field = value }
        get() { ++MyDemoOpenGetterCalls; return field }
}

// FILE: MyDemo2.kt
var MyDemo2SetterCalls = 0
var MyDemo2GetterCalls = 0
var MyDemo2OpenSetterCalls = 0
var MyDemo2OpenGetterCalls = 0

class MyDemo2 : LibDemo() {
    fun demoFun(): Int = 1000
    val demoVal: Int = 2000
    val demoValGet: Int
        get() = 3000
    var demoVarSetGet: Int = 4000
        set(value) { ++MyDemo2SetterCalls; field = value }
        get() { ++MyDemo2GetterCalls; return field }

    override fun demoOpenFun(): Int = 5000
    override val demoOpenVal: Int = 6000
    override val demoOpenValGet: Int
        get() = 7000
    override var demoOpenVarSetGet: Int = 8000
        set(value) { ++MyDemo2OpenSetterCalls; field = value }
        get() { ++MyDemo2OpenGetterCalls; return field }
}

// FILE: Box.kt
import kotlin.test.assertEquals

fun resetCounters() {
    DemoSetterCalls = 0
    DemoGetterCalls = 0
    DemoOpenSetterCalls = 0
    DemoOpenGetterCalls = 0

    LibDemoOpenSetterCalls = 0
    LibDemoOpenGetterCalls = 0

    MyDemoSetterCalls = 0
    MyDemoGetterCalls = 0
    MyDemoOpenSetterCalls = 0
    MyDemoOpenGetterCalls = 0

    MyDemo2SetterCalls = 0
    MyDemo2GetterCalls = 0
    MyDemo2OpenSetterCalls = 0
    MyDemo2OpenGetterCalls = 0
}

fun testMyDemo() {
    resetCounters()
    val myDemo = MyDemo()

    assertEquals(testFun(myDemo), 1)
    assertEquals(testVal(myDemo), 2)
    assertEquals(testValGet(myDemo), 3)
    assertEquals(testVarGet(myDemo), 4)
    testVarSet(myDemo, -4)
    assertEquals(testVarGet(myDemo), -4)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)
    assertEquals(MyDemoSetterCalls, 0)
    assertEquals(MyDemoGetterCalls, 0)

    assertEquals(testOpenFun(myDemo), 5)
    assertEquals(testOpenVal(myDemo), 6)
    assertEquals(testOpenValGet(myDemo), 7)
    assertEquals(testOpenVarGet(myDemo), 8)
    testOpenVarSet(myDemo, -8)
    assertEquals(testOpenVarGet(myDemo), -8)
    assertEquals(DemoOpenSetterCalls, 1)
    assertEquals(DemoOpenGetterCalls, 2)
    assertEquals(MyDemoOpenSetterCalls, 0)
    assertEquals(MyDemoOpenGetterCalls, 0)

    assertEquals(myDemo.demoFun(), 100)
    assertEquals(myDemo.demoVal, 200)
    assertEquals(myDemo.demoValGet, 300)
    assertEquals(myDemo.demoVarSetGet, 400)
    myDemo.demoVarSetGet = -400
    assertEquals(myDemo.demoVarSetGet, -400)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)
    assertEquals(MyDemoSetterCalls, 1)
    assertEquals(MyDemoGetterCalls, 2)

    assertEquals(myDemo.demoOpenFun(), 500)
    assertEquals(myDemo.demoOpenVal, 600)
    assertEquals(myDemo.demoOpenValGet, 700)
    assertEquals(myDemo.demoOpenVarSetGet, 800)
    myDemo.demoOpenVarSetGet = -800
    assertEquals(myDemo.demoOpenVarSetGet, -800)
    assertEquals(DemoOpenSetterCalls, 1)
    assertEquals(DemoOpenGetterCalls, 2)
    assertEquals(MyDemoOpenSetterCalls, 1)
    assertEquals(MyDemoOpenGetterCalls, 2)
}

fun testLibDemo() {
    resetCounters()
    val libDemo = LibDemo()

    assertEquals(testFun(libDemo), 1)
    assertEquals(testVal(libDemo), 2)
    assertEquals(testValGet(libDemo), 3)
    assertEquals(testVarGet(libDemo), 4)
    testVarSet(libDemo, -4)
    assertEquals(testVarGet(libDemo), -4)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)

    assertEquals(testOpenFun(libDemo), 50)
    assertEquals(testOpenVal(libDemo), 60)
    assertEquals(testOpenValGet(libDemo), 70)
    assertEquals(testOpenVarGet(libDemo), 80)
    testOpenVarSet(libDemo, -80)
    assertEquals(testOpenVarGet(libDemo), -80)
    assertEquals(LibDemoOpenSetterCalls, 1)
    assertEquals(LibDemoOpenGetterCalls, 2)
    assertEquals(DemoOpenSetterCalls, 0)
    assertEquals(DemoOpenGetterCalls, 0)

    assertEquals(libDemo.demoOpenFun(), 50)
    assertEquals(libDemo.demoOpenVal, 60)
    assertEquals(libDemo.demoOpenValGet, 70)
    assertEquals(libDemo.demoOpenVarSetGet, -80)
    libDemo.demoOpenVarSetGet = 88
    assertEquals(libDemo.demoOpenVarSetGet, 88)
    assertEquals(LibDemoOpenSetterCalls, 2)
    assertEquals(LibDemoOpenGetterCalls, 4)
    assertEquals(DemoOpenSetterCalls, 0)
    assertEquals(DemoOpenGetterCalls, 0)
}

fun testMyDemo2() {
    resetCounters()
    val myDemo2 = MyDemo2()

    assertEquals(testFun(myDemo2), 1)
    assertEquals(testVal(myDemo2), 2)
    assertEquals(testValGet(myDemo2), 3)
    assertEquals(testVarGet(myDemo2), 4)
    testVarSet(myDemo2, -4)
    assertEquals(testVarGet(myDemo2), -4)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)
    assertEquals(MyDemo2SetterCalls, 0)
    assertEquals(MyDemo2GetterCalls, 0)

    assertEquals(testOpenFun(myDemo2), 5000)
    assertEquals(testOpenVal(myDemo2), 6000)
    assertEquals(testOpenValGet(myDemo2), 7000)
    assertEquals(testOpenVarGet(myDemo2), 8000)
    testOpenVarSet(myDemo2, -8000)
    assertEquals(testOpenVarGet(myDemo2), -8000)
    assertEquals(MyDemo2OpenSetterCalls, 1)
    assertEquals(MyDemo2OpenGetterCalls, 2)
    assertEquals(LibDemoOpenSetterCalls, 0)
    assertEquals(LibDemoOpenGetterCalls, 0)
    assertEquals(DemoOpenSetterCalls, 0)
    assertEquals(DemoOpenGetterCalls, 0)

    assertEquals(myDemo2.demoFun(), 1000)
    assertEquals(myDemo2.demoVal, 2000)
    assertEquals(myDemo2.demoValGet, 3000)
    assertEquals(myDemo2.demoVarSetGet, 4000)
    myDemo2.demoVarSetGet = -4000
    assertEquals(myDemo2.demoVarSetGet, -4000)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)
    assertEquals(MyDemo2SetterCalls, 1)
    assertEquals(MyDemo2GetterCalls, 2)

    assertEquals(myDemo2.demoOpenFun(), 5000)
    assertEquals(myDemo2.demoOpenVal, 6000)
    assertEquals(myDemo2.demoOpenValGet, 7000)
    assertEquals(myDemo2.demoOpenVarSetGet, -8000)
    myDemo2.demoOpenVarSetGet = 8888
    assertEquals(myDemo2.demoOpenVarSetGet, 8888)
    assertEquals(MyDemo2OpenSetterCalls, 2)
    assertEquals(MyDemo2OpenGetterCalls, 4)
    assertEquals(LibDemoOpenSetterCalls, 0)
    assertEquals(LibDemoOpenGetterCalls, 0)
    assertEquals(DemoOpenSetterCalls, 0)
    assertEquals(DemoOpenGetterCalls, 0)
}

fun box(): String {
    testMyDemo()
    testLibDemo()
    testMyDemo2()
    return "OK"
}
