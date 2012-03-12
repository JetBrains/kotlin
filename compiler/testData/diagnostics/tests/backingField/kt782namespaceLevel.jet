// KT-782 Allow backing field usage for accessors of variables on namespace level

package kt782

val z : Int = 34

val y : Int = 11
get() {
    return $y
}

val x : Int
get() = z

val w : Int
get() = <!INACCESSIBLE_BACKING_FIELD!>$z<!>

fun foo() {
    <!INACCESSIBLE_BACKING_FIELD!>$y<!> = 34
}
