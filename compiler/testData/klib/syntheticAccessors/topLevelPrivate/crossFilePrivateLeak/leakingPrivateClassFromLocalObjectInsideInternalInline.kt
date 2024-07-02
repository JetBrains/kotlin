// IGNORE_BACKEND: ANY

// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = object : A() {
    fun foo() = super.ok
}.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
