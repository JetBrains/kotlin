class A {
    <caret>inner class B {

        fun b() {}

        inner class C {
            fun c() {
                b()
            }
        }
    }
}