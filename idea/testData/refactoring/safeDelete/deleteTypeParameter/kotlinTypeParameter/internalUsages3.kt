class A<<caret>X, Y: X, Z> {
    fun foo(x: X) {

    }
}

val a = A<Int, String, Any>()