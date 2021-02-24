// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package m

import checkSubtype

fun test(i: Int?) {
    if (i != null) {
        foo(l1@ i)
        foo((i))
        foo(l2@ (i))
        foo((l3@ i))
    }

    val a: Int = l4@ ""
    val b: Int = ("")
    val c: Int = <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>("")
    val d: Int = <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Long>("")


    <!INAPPLICABLE_CANDIDATE!>foo<!>(l4@ "")
    <!INAPPLICABLE_CANDIDATE!>foo<!>((""))
    foo(<!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(""))
    <!INAPPLICABLE_CANDIDATE!>foo<!>(<!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Long>(""))
    
    use(a, b, c, d)
}

fun foo(i: Int) = i

fun use(vararg a: Any?) = a
