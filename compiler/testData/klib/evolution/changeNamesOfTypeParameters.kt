// MODULE: lib
// FILE: A.kt
// VERSION: 1

interface B<T> {
    var few: T
}

class A<X, Y, Z, W>(z: Z, w: W): B<W> {
    var bar: Z = z
    override var few: W = w
    fun foo(x: X): Y {
        return x as Y
    }
}

fun <U, V> qux(u: U): V {
    return u as V
}

// FILE: B.kt
// VERSION: 2

interface B<T1> {
    var few: T1
}

class A<X1, Y1, Z1, W1>(z: Z1, w: W1): B<W1> {
    var bar: Z1 = z
    override var few: W1 = w
    fun foo(x: X1): Y1 {
        return x as Y1
    }
}

fun <U1, V1> qux(u: U1): V1 {
    return u as V1
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String {
    val a = A<String, String, Int, Int>(19, 17)

    return when {
        a.foo("first") != "first" -> "fail 1"
        a.bar != 19 -> "fail 2"
        a.few != 17 -> "fail 3"
        qux<String, String>("second") != "second" -> "fail 4"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

