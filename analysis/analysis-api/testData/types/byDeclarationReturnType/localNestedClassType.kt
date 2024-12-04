class A {
    fun aa() {
        class B {
            class C
        }

        fun cc<caret>c(): B.C
    }
}