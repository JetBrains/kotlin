// vtable call + interface call
interface Z {
    fun foo(): Any
}

interface Y {
    fun foo(): Int
}

open class A {
    fun foo(): Int = 42
}

open class B: A(), Z, Y

fun main(args: Array<String>) {
    val z: Z = B()
    val y: Y = z as Y
    println(z.foo().toString())
    println(y.foo().toString())
}