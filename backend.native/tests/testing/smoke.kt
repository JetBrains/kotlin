import kotlin.test.Test
import konan.test.*

class A {
    @Test
    fun test() {
        println("A.test")
    }
}

@Test
fun test() {
    println("test")
}

