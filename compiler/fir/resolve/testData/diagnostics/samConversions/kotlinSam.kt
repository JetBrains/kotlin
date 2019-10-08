// TODO: These interfaces must be marked as "fun" ones that modifier is supported
interface MyRunnable {
    fun foo(x: Int): Boolean
}

interface WithProperty {
    val x: Int
}

interface TwoAbstract : MyRunnable {
    fun bar()
}

interface Super {
    fun foo(x: Int): Any
}

interface Derived : Super {
    override fun foo(x: Int): Boolean
}

fun foo1(m: MyRunnable) {}
fun foo2(m: WithProperty) {}
fun foo3(m: TwoAbstract) {}
fun foo3(m: Derived) {}

fun main() {
    val f = { t: Int -> t > 1}

    foo1 { x -> x > 1 }
    foo1(f)

    foo2 { x -> x > 1 }
    foo2(f)

    foo3 { x -> x > 1 }
    foo3(f)

    foo4 { x -> x > 1 }
    foo4(f)

}
