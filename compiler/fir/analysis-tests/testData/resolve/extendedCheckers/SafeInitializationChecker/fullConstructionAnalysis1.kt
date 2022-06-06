abstract class A {
    val y = foo().hashCode()

    abstract fun foo(): String
}

class ErrorImpl : A() {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val x = "Hello"<!>
    override fun foo(): String = x
}

class CorrectImpl : A() {
    val x = "Hello"
    override fun foo(): String = "World"
}

// KT-43019
open class C(<!REDUNDANT_MODALITY_MODIFIER!>open<!> val v: String) {
    val present = v.hashCode()
}

class D(<!ACCESS_TO_UNINITIALIZED_VALUE!>override val v: String<!>) : C(v)


