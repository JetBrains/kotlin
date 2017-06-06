// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

protocol interface Proto {
    fun foo(i: Int): Int
}

protocol interface GProto<T> {
    fun foo(t: T): T
}

class X<T> {
    fun foo(t: T): T = t
}

class Y {
    fun foo(i: Int): Int = i
}

protocol interface Super {
    fun <T> foo(t: T)
}

class Z {
    fun <X> foo(b : X) {}
}


fun test() {
    val a: GProto<Int> = X<Int>()
    val b: Proto = X<Int>()
    val c: GProto<Int> = Y()

    val d: GProto<Any> = <!TYPE_MISMATCH!>X<Int>()<!>
    val e: GProto<Int> = <!TYPE_MISMATCH!>X<Any>()<!>
    val f: GProto<Any> = <!TYPE_MISMATCH!>Y()<!>

    val g: Super = Z()
}
