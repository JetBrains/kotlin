interface Base {
    fun foo()
}

class Derived : Base() {
    fun f<caret>oo() {

    }
}
