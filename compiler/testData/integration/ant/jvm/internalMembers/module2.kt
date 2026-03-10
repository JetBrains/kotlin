package hello

open class B : A() {

    internal val z: String = "B_O"

    internal fun test() = "B_K"

}

class C : A() {

    public val z: String = "C_O"

    public fun test() = "C_K"

}


public fun invokeOnB(b: B) = b.z + b.test()

public fun invokeOnC(c: C) = c.z + c.test()

fun main() {
    val b = B()
    println(invokeOnA(b))
    println(invokeOnB(b))
    println(invokeOnC(C()))
}