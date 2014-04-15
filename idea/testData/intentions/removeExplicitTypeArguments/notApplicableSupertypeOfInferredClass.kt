// IS_APPLICABLE: false
fun foo() {
    val x = <caret>Box<Any>("x")
}

class Box<T>(t : T) {
    var value = t
}