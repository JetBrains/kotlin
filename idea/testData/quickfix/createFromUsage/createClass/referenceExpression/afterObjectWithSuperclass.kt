// "Create object 'A'" "true"
package p

fun foo(): X = A

open class X {

}

object A : X() {

}
