// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
//KT-2216 Nullability of a value determined in function parameter computation doesn't pass to code following
package kt2216

fun bar(y: Int, z: Int) = y + z
fun baz(a: Int, b: Int, c: Int, d: Int) = a + b + c + d

fun foo() {
    val x: Int? = 0

    bar(if (x != null) x else return, x)
    x + 2
    bar(x, x!!)

    val y: Int? = 0
    val z: Int? = 0
    <!INAPPLICABLE_CANDIDATE!>bar<!>(if (y != null) y else z, y)
    y <!NONE_APPLICABLE!>+<!> 2
    <!INAPPLICABLE_CANDIDATE!>baz<!>(y, y, if (y == null) return else y, y)
    baz(y, z!!, z, y)
}
