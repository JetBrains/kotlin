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
    u!!.b<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    x?.b!!.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    // x?.b is not null
    x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    y?.nb?.foo()!!
    y!!.nb?.foo()!!
    z?.nb!!.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    // z?.nb is not null
    z<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.nb!!.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    w.b<!UNNECESSARY_SAFE_CALL!>?.<!>foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    w.b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    w.nb?.foo()!!
    w.nb!!.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    v!!.b.foo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
}

fun B?.bar(): Int = 1
fun B?.baz(): Int? = 1

fun doInt(i: Int) = i

fun test(a: A?) {
    doInt(a?.b.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    doInt(a?.b.baz()!!)
}
