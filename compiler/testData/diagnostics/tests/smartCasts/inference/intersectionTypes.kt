// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package a

fun <T> id(t: T): T = t

fun <T> two(u: T, <!UNUSED_PARAMETER!>v<!>: T): T = u

fun <T> three(<!UNUSED_PARAMETER!>a<!>: T, <!UNUSED_PARAMETER!>b<!>: T, c: T): T = c

interface A
interface B: A
interface C: A

fun test(a: A, b: B, c: C) {
    if (a is B && a is C) {
        val d: C = id(<!DEBUG_INFO_SMARTCAST!>a<!>)
        val e: Any = id(a)
        val f = id(a)
        checkSubtype<A>(f)
        val g = two(<!DEBUG_INFO_SMARTCAST!>a<!>, b)
        checkSubtype<B>(g)
        checkSubtype<A>(g)

        // smart cast isn't needed, but is reported due to KT-4294
        val h: Any = two(<!DEBUG_INFO_SMARTCAST!>a<!>, b)

        val k = three(a, b, c)
        checkSubtype<A>(k)
        checkSubtype<B>(<!TYPE_MISMATCH!>k<!>)
        val l: Int = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>three(a, b, c)<!>
        
        use(d, e, f, g, h, k, l)
    }
}

fun <T> foo(t: T, <!UNUSED_PARAMETER!>l<!>: MutableList<T>): T = t

fun testErrorMessages(a: A, ml: MutableList<String>) {
    if (a is B && a is C) {
        <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>foo<!>(a, ml)
    }

    if(a is C) {
        <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>foo<!>(a, ml)
    }
}

fun rr(s: String?) {
    if (s != null) {
        val l = arrayListOf("", <!DEBUG_INFO_SMARTCAST!>s<!>)
        checkSubtype<MutableList<String>>(l)
        checkSubtype<MutableList<String?>>(<!TYPE_MISMATCH!>l<!>)
    }
}

//from library
fun <T> arrayListOf(vararg <!UNUSED_PARAMETER!>values<!>: T): MutableList<T> = throw Exception()

fun use(vararg a: Any) = a