interface I {
    fun foo1()
    fun foo2()
    fun foo3()
}

abstract class Base1 : I
abstract class Base2 : I

abstract class A : Base1() {
    override fun foo2() {
    }
}

abstract class B(val a: A) : Base2() {
    override fun foo2() {
        a.<caret>
    }
}

// ORDER: foo2
// ORDER: foo1
// ORDER: foo3