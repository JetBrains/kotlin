// "Specify override for 'foo(): Unit' explicitly" "false"
// ACTION: Convert to secondary constructor
// ACTION: Create test
// ACTION: Make primary constructor internal
// ACTION: Make primary constructor private
// ACTION: Move 'C' to separate file
// ACTION: Rename file to C.kt

interface A {
    fun foo()
}

class W(val a: A)

open class B : A {
    override fun foo() {}
}

class C<caret>(w: W) : B(), A by w.a