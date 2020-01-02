interface A {
    fun f(): String = "string"
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

class C : B(), A

val obj: A = object : B(), A {}
