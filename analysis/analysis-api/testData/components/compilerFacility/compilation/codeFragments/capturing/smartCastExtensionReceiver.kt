fun test() {
    dumbWith("Hello, world!") {
        if (this is String) {
            dumbWith(Foo()) {
                if (this is Foo) {
                    <caret>val x = 0
                }
            }
        }
    }
}

inline fun dumbWith(obj: Any?, block: Any?.() -> Unit) {
    obj.block()
}

class Foo {
    val foo: String = "foo"
}