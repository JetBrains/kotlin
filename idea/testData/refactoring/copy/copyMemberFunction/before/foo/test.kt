package foo

class A {
    fun <caret>b() {

    }

    init {
        b()
    }
}