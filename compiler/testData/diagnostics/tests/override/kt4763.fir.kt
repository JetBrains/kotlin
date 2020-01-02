interface A {
    fun f(): String
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

class C : B(), A

val d: A = object : B(), A {}
