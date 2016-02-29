// FILE: A.kt

class A(vararg s: String) {

}

// FILE: B.kt

fun box(): String {
    A()
    A("a")
    A("a", "b")
    return "OK"
}
