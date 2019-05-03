// "Specify override for 'foo(): Unit' explicitly" "false"
// ACTION: Create test
// ACTION: Make primary constructor internal
// ACTION: Make primary constructor private
// ACTION: Extract 'C' from current file
// ACTION: Rename file to C.kt

interface A {
    fun foo()
}

class W(val a: A)

open class B : A {
    override fun foo() {}
}

class C<caret>(w: W) : B(), A by w.a