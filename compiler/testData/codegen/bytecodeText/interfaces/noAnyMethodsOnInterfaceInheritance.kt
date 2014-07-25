// If we generate equals/hashCode/toString in all interfaces, there will be too much bytecode

interface A {
    fun foo(): Number = 42
}

interface B : A

class C : B {
    override fun foo(): Int = super.foo() as Int
}

// 0 equals
// 0 hashCode
// 0 toString
