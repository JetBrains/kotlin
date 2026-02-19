open class C1 {
    open fun foo() {
    }
}

class C2 : C1() {
    inner class Inner {
        fun bar() {
            <expr>super@C2</expr>.foo()
        }
    }
}
