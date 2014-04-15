// IS_APPLICABLE: true
fun foo() {
    val x = <caret>Box<String>("x")
}

class Box<T>(t : T) {
    var value = t
}