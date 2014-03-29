// IS_APPLICABLE: true
fun foo() {
    val x = <caret>Box<Box<String>>(Box("x"))
}

class Box<T>(t : T) {
    var value = t
}