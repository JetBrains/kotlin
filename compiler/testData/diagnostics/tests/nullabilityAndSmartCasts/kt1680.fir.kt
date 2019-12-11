//KT-1680 Warn if non-null variable is compared to null
package kt1680

fun foo() {
    val x = 1
    if (x != null) {} // <-- need a warning here!
    if (x == null) {}
    if (null != x) {}
    if (null == x) {}

    val y : Int? = 1
    if (y != null) {}
    if (y == null) {}
}