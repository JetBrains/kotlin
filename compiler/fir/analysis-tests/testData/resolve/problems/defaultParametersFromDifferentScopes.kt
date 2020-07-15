// See KT-40327

interface Foo {
    fun some(x: Int = 1, y: Int)
}

interface Bar {
    fun some(x: Int, y: Int = 2)
}

class Impl : Foo, Bar {
    override fun some(x: Int, y: Int) {
        // println("x = $x y = $y")
    }
}

fun main() {
    Impl().some()
}
