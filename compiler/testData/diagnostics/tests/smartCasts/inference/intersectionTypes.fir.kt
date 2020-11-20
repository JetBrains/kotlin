// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package a

import checkSubtype

fun <T> id(t: T): T = t

fun <T> two(u: T, v: T): T = u

fun <T> three(a: T, b: T, c: T): T = c

interface A
interface B: A
interface C: A

fun test(a: A, b: B, c: C) {
    if (a is B && a is C) {
        val d: C = id(a)
        val e: Any = id(a)
        val f = id(a)
        checkSubtype<A>(f)
        val g = two(a, b)
        checkSubtype<B>(g)
        checkSubtype<A>(g)

        // smart cast isn't needed, but is reported due to KT-4294
        val h: Any = two(a, b)

        val k = three(a, b, c)
        checkSubtype<A>(k)
        <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><B>(k)
        val l: Int = three(a, b, c)
        
        use(d, e, f, g, h, k, l)
    }
}

fun <T> foo(t: T, l: MutableList<T>): T = t

fun testErrorMessages(a: A, ml: MutableList<String>) {
    if (a is B && a is C) {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(a, ml)
    }

    if(a is C) {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(a, ml)
    }
}

fun rr(s: String?) {
    if (s != null) {
        val l = arrayListOf("", s)
        checkSubtype<MutableList<String>>(l)
        <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><MutableList<String?>>(l)
    }
}

//from library
fun <T> arrayListOf(vararg values: T): MutableList<T> = throw Exception()

fun use(vararg a: Any) = a
