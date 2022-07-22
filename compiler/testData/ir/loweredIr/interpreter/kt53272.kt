// FILE: 1.kt

const val name = E.OK.name
fun box(): String = name

// FILE: 2.kt

enum class E(val parent: E?) {
    X(null),
    OK(X),
}