// IGNORE_BACKEND: WASM
// WITH_STDLIB
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61384

// MODULE: lib1

// FILE: DemoClassInternal1.kt
abstract class DemoClassInternal1 {
    internal open fun demo(): Int = 1
}

interface DemoInterface1 {
    fun demo(): Int = 2
}

// FILE: TestDemo1.kt
fun testDemoClassInternal1(d: DemoClassInternal1): Int = d.demo()
fun testDemoInterface1(d: DemoInterface1): Int = d.demo()

// MODULE: lib2

// FILE: DemoClassInternal2.kt
abstract class DemoClassInternal2 {
    internal open fun demo(): Int = 3
}

interface DemoInterface2 {
    fun demo(): Int = 4
}

// FILE: TestDemo.kt
fun testDemoClassInternal2(d: DemoClassInternal2): Int = d.demo()
fun testDemoInterface2(d: DemoInterface2): Int = d.demo()

// MODULE: main(lib1)(lib2)

// FILE: MyDemo.kt
class MyDemo1 : DemoClassInternal1(), DemoInterface1, DemoInterface2 {
    override fun demo(): Int = 10 * super<DemoInterface1>.demo() + super<DemoInterface2>.demo()
}

class MyDemo2 : DemoClassInternal2(), DemoInterface1, DemoInterface2 {
    override fun demo(): Int = 100 * super<DemoClassInternal2>.demo() + 10 * super<DemoInterface1>.demo() + super<DemoInterface2>.demo()
}

// FILE: Box.kt
import kotlin.test.assertEquals

fun testMyDemo1() {
    val myDemo = MyDemo1()

    assertEquals(testDemoClassInternal1(myDemo), 1)
    assertEquals(testDemoInterface1(myDemo), 24)
    assertEquals(testDemoInterface2(myDemo), 24)
    assertEquals(myDemo.demo(), 24)
}

fun testMyDemo2() {
    val myDemo = MyDemo2()

    assertEquals(testDemoClassInternal2(myDemo), 324)
    assertEquals(testDemoInterface1(myDemo), 324)
    assertEquals(testDemoInterface2(myDemo), 324)
    assertEquals(myDemo.demo(), 324)
}

fun box(): String {
    testMyDemo1()
    testMyDemo2()
    return "OK"
}
