interface X {
    fun foo(): Int = 42
}

interface Y

class A: X, Y

class B: X, Y

class Out<out T: X>(val x: T)

fun bar(a: Out<A>, b: Out<B>, f: Boolean): Int {
    val x = if (f) a else b
    return x.x.foo()    
}