interface Base

class Child: Base

interface Recursive<T: Recursive<T>>

fun <T: R, R: I, I: Recursive<T>> Base.foo(x: T, y: R, z: I): T = x

/**
 * [Child.f<caret_1>oo]
 * [Base.fo<caret_2>o]
 */
fun usage() {}