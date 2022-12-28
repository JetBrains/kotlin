// FIR_IDENTICAL
// MODULE: m1
// FILE: A.kt

open class A {
    internal open fun foo() : Int = 1
}

open class AG<T> {
    internal open fun bar(arg: T) = arg
}

// MODULE: m2(m1)
// FILE: B.kt

class B : A() {
    fun foo() : String = ""
}

class BG : AG<String>() {
    fun bar(arg: Int) = arg
}
