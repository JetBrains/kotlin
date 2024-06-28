// WITH_STDLIB

// MODULE: lib1

// FILE: DemoClassInternal1.kt
abstract class DemoClassInternal {
    @PublishedApi
    internal open fun demo(): Int = 1
}

// FILE: TestDemo1.kt
inline fun testDemoInline(d: DemoClassInternal): Int = d.demo()
fun testDemo(d: DemoClassInternal): Int = d.demo()

// MODULE: main(lib1)

// FILE: MyDemo.kt
open class MyDemo1 : DemoClassInternal()

class MyDemo2 : DemoClassInternal() {
    fun demo(): Int = 2
}

open class MyDemo3 : DemoClassInternal() {
    open fun demo(): Int = 3
}

class MyDemo4: MyDemo1()

class MyDemo5: MyDemo1() {
    fun demo(): Int = 5
}

class MyDemo6: MyDemo3()

class MyDemo7: MyDemo3() {
    override fun demo(): Int = 7
}

// FILE: Box.kt
import kotlin.test.assertEquals

fun testMyDemo1() {
    val myDemo = MyDemo1()

    assertEquals(testDemoInline(myDemo), 1)
    assertEquals(testDemo(myDemo), 1)
}

fun testMyDemo2() {
    val myDemo = MyDemo2()

    assertEquals(testDemoInline(myDemo), 2)
    assertEquals(testDemo(myDemo), 2)
    assertEquals(myDemo.demo(), 2)
}

fun testMyDemo3() {
    val myDemo = MyDemo3()

    assertEquals(testDemoInline(myDemo), 3)
    assertEquals(testDemo(myDemo), 3)
    assertEquals(myDemo.demo(), 3)
}

fun testMyDemo4() {
    val myDemo = MyDemo4()

    assertEquals(testDemoInline(myDemo), 1)
    assertEquals(testDemo(myDemo), 1)
}

fun testMyDemo5() {
    val myDemo = MyDemo5()

    assertEquals(testDemoInline(myDemo), 5)
    assertEquals(testDemo(myDemo), 5)
    assertEquals(myDemo.demo(), 5)
}

fun testMyDemo6() {
    val myDemo = MyDemo6()

    assertEquals(testDemoInline(myDemo), 3)
    assertEquals(testDemo(myDemo), 3)
    assertEquals(myDemo.demo(), 3)
}

fun testMyDemo7() {
    val myDemo = MyDemo7()

    assertEquals(testDemoInline(myDemo), 7)
    assertEquals(testDemo(myDemo), 7)
    assertEquals(myDemo.demo(), 7)
}

fun box(): String {
    testMyDemo1()
    testMyDemo2()
    testMyDemo3()
    testMyDemo4()
    testMyDemo5()
    testMyDemo6()
    testMyDemo7()
    return "OK"
}
