class B {
    fun append() {}
}

class A {
    val message = B()

    fun foo(w: Boolean) {
        if (w) {
            val message = ""
            message.toString()
        } else {
            message.append() // message here should relate to the class-level property
        }
    }
}
