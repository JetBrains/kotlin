// !WITH_NEW_INFERENCE
//KT-2164 !! does not propagate nullability information
package kt2164

fun foo(x: Int): Int = x + 1

fun main() {
    val x: Int? = null

    foo(<!TYPE_MISMATCH!>x<!>)

    if (x != null) {
        foo(<!DEBUG_INFO_SMARTCAST!>x<!>)
        foo(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        foo(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }

    foo(<!TYPE_MISMATCH!>x<!>)

    if (x != null) {
        foo(<!DEBUG_INFO_SMARTCAST!>x<!>)
        foo(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        foo(<!DEBUG_INFO_SMARTCAST!>x<!>)
    } else {
        foo(<!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>x<!>)
        <!OI;UNREACHABLE_CODE!>foo(<!><!ALWAYS_NULL!>x<!>!!<!OI;UNREACHABLE_CODE!>)<!>
        <!OI;UNREACHABLE_CODE!>foo(<!DEBUG_INFO_SMARTCAST!>x<!>)<!>
    }

    foo(<!DEBUG_INFO_SMARTCAST!>x<!>)
    foo(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    foo(<!DEBUG_INFO_SMARTCAST!>x<!>)
    
    val y: Int? = null
    y!!
    y<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    foo(<!DEBUG_INFO_SMARTCAST!>y<!>)
    foo(y<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}
