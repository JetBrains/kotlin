// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

// MODULE: common
expect open class A()

// MODULE: intermediate()()(common)
class B : A() {
    fun foo(): String = "O"
}

fun getB(): B = B()

// MODULE: main()()(intermediate)
actual open class A actual constructor() {
    fun bar(): String = "K"
}

fun box(): String {
    val b = getB()
    return b.foo() + b.bar()
}
