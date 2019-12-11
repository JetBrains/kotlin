// !WITH_NEW_INFERENCE
//KT-2164 !! does not propagate nullability information
package kt2164

fun foo(x: Int): Int = x + 1

fun main() {
    val x: Int? = null

    foo(x)

    if (x != null) {
        foo(x)
        foo(x!!)
        foo(x)
    }

    foo(x)

    if (x != null) {
        foo(x)
        foo(x!!)
        foo(x)
    } else {
        foo(x)
        foo(x!!)
        foo(x)
    }

    foo(x)
    foo(x!!)
    foo(x)
    
    val y: Int? = null
    y!!
    y!!
    foo(y)
    foo(y!!)
}
