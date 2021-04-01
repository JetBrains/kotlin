// !WITH_NEW_INFERENCE
//KT-2164 !! does not propagate nullability information
package kt2164

fun foo(x: Int): Int = x + 1

fun main() {
    val x: Int? = null

    <!INAPPLICABLE_CANDIDATE!>foo<!>(x)

    if (x != null) {
        foo(x)
        foo(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        foo(x)
    }

    <!INAPPLICABLE_CANDIDATE!>foo<!>(x)

    if (x != null) {
        foo(x)
        foo(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        foo(x)
    } else {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(x)
        foo(x!!)
        foo(x)
    }

    foo(x)
    foo(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    foo(x)
    
    val y: Int? = null
    y!!
    y<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    foo(y)
    foo(y<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}
