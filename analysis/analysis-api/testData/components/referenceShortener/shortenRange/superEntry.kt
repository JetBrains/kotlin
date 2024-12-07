package foo.bar
interface A {
    fun foo(i: Int) {}
}

interface B {
    fun foo(i2: Int) {}
}

class C : A, B {
    override fun foo(j: Int) {
        super<<expr>foo.bar.A</expr>>.foo()
    }
}