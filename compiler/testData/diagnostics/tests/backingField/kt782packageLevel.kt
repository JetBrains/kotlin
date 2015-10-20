// KT-782 Allow backing field usage for accessors of variables on namespace level

package kt782

val z : Int = 34

val y : Int = 11
get() {
    return field
}

val x : Int
get() = z

val w : Int = 56
get() = field