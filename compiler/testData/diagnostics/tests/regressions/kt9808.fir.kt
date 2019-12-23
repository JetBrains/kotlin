// KT-9808 Extension function on object for new resolve
object O

val foo: O.() -> Unit  = null!!

fun test() {
    O.foo()
}