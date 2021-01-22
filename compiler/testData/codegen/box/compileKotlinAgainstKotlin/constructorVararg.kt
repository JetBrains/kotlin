// MODULE: lib
// FILE: A.kt

class A(vararg s: String) {

}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    A()
    A("a")
    A("a", "b")
    return "OK"
}
