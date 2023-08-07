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
class MyDemo : DemoClassInternal() {
    fun demo(): Int = 2
}

// FILE: Box.kt
import kotlin.test.assertEquals

fun testMyDemo() {
    val myDemo = MyDemo()

    assertEquals(testDemoInline(myDemo), 2)
    assertEquals(testDemo(myDemo), 2)
    assertEquals(myDemo.demo(), 2)
}

fun box(): String {
    testMyDemo()
    return "OK"
}
