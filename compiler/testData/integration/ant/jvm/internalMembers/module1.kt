package hello

open class A {

    internal val z: String = "A_O"

    internal fun test() = "A_K"

}

public fun invokeOnA(a: A) = a.z + a.test()