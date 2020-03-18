interface B {
    fun foo(): Int
}

class A {
    val String.x: Int get() {
        return field.foo()
    }

    val String.field: B get() = TODO()
}
