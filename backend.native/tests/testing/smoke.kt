import kotlin.test.Test

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

