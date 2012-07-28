//KT-607 Val reassignment is not marked as an error

package kt607

fun foo(a: A) {
    val o = object {
        val y : Int
           get() = 42
    }

    <!VAL_REASSIGNMENT!>a.z<!> = 23
    <!VAL_REASSIGNMENT!>o.y<!> = 11   //Should be an error here
}

class A() {
    val z : Int
    get() = 3
}
