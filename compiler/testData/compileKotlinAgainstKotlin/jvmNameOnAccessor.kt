// FILE: A.kt

class A {
    val OK: String = "OK"
        @JvmName("OK") get
}

// FILE: B.kt

fun main(args: Array<String>) {
    if (A().OK != "OK") throw java.lang.AssertionError()
}
