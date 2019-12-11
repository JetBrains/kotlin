class B {
    companion object A {
    }

    val A = this
}

class C {
    companion object A {
        val A = this
    }

}
