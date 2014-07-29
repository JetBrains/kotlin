class A {
    inner class B {
        inner class C {
            fun foo() {
                this
                this@A
                <selection>this@B</selection>
                this@C
            }
        }

        fun foo() {
            this
            this@A
            this@B
        }
    }

    fun foo() {
        this
        this@A
    }
}