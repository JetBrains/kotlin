package a

interface A {
    val b: B
    val nb: B?
}

interface B {
    fun foo(): Int
}

fun test(u: A?, x: A?, y: A?, z: A?, w: A, v: A?) {
    u?.b?.foo()!! // was UNNECESSARY_SAFE_CALL everywhere, because result type (of 'foo()') wasn't made nullable
    u!!.b?.foo()!!
    x?.b!!.foo()!!
    // x?.b is not null
    x!!.b!!.foo()!!

    y?.nb?.foo()!!
    y!!.nb?.foo()!!
    z?.nb!!.foo()!!
    // z?.nb is not null
    z!!.nb!!.foo()!!

    w.b?.foo()!!
    w.b!!.foo()!!
    w.nb?.foo()!!
    w.nb!!.foo()!!

    v!!.b.foo()!!
}

fun B?.bar(): Int = 1
fun B?.baz(): Int? = 1

fun doInt(i: Int) = i

fun test(a: A?) {
    doInt(a?.b.bar()!!)
    doInt(a?.b.baz()!!)
}