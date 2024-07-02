// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = object : A() {
    fun foo() = super.ok
}.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
