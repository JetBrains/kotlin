fun regular() {
    ::class

    with(Any()) {
        ::class
    }
}

fun Any.extension() {
    ::class
}

class A {
    fun member() {
        ::class
    }
}